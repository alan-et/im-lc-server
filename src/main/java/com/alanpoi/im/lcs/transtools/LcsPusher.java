package com.alanpoi.im.lcs.transtools;

import com.qzd.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.network.Frame;
import com.alanpoi.im.lcs.transtools.network.TcpClient;
import io.netty.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS服务推送类
 */
public class LcsPusher {
    private static final Logger log = LoggerFactory.getLogger(LcsPusher.class);

    public static final int ERROR_UNKNOWN = 1;
    public static final int ERROR_OFFLINE = 2;
    public static final int ERROR_TIMEOUT = 3;
    public static final int ERROR_OFFER_QUEUE = 4;

    private static final int PUSH_QUEUE_LENGTH = 1_000_000; //推送队列的长度 100w
    private static final int PUSH_TIMEOUT = 3000; //推送超时时间

    private LcsFinder lcsFinder;
    private ThreadPoolExecutor executor;
    private EventExecutorGroup exeGroup;
    private BlockingQueue<PushRequest> pushQueue;

    
    public LcsPusher(int threads, LcsFinder finder){
        this.lcsFinder = finder;

        executor = new ThreadPoolExecutor(threads, threads, 300, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000_000), new DefaultThreadFactory("LcsPusher"));

        exeGroup = new DefaultEventExecutorGroup(4, new DefaultThreadFactory("LcsPusher-ee"));

        pushQueue = new ArrayBlockingQueue<>(PUSH_QUEUE_LENGTH);
    }

    public Map<PersonId, Set<String>> getOnlinePersons(List<PersonId> personIds){
        return lcsFinder.getOnlinePersons(personIds);
    }
    
    public Promise<List<byte[]>> push(PersonId personId, byte[] data){
        Promise<List<byte[]>> promise = new DefaultPromise<>(exeGroup.next());

        PushRequest pr = new PushRequest();
        pr.setPersonId(personId);
        pr.setData(data);
        pr.setTimeStamp(System.currentTimeMillis());
        pr.setPromise(promise);

        boolean succ = pushQueue.offer(pr);
        if (!succ) {
            log.warn("offer error {}", pr.getPersonId());
            promise.tryFailure(new PushException(ERROR_OFFER_QUEUE, "offer queue error"));
        }


        if(executor.getActiveCount() < executor.getCorePoolSize()){
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            doPush();
                        }catch (Throwable e){
                            log.error("doPush error ",e);
                        }
                    }
                });
            }catch (Exception e){
                log.error("LcsPusher push error ", e);
            }
        }

        return promise;
    }

    private void doPush(){
        final int batchCount = 500;
        List<PushRequest> prs = new ArrayList<>(batchCount);
        Set<PersonId> personIds = new HashSet<>();

        long curt = System.currentTimeMillis();
        while(pushQueue.size() > 0){
            prs.clear();
            for(int i = 0; i < batchCount; ++ i){
                PushRequest pr = pushQueue.poll();
                if(null == pr) break;
                if(curt - pr.getTimeStamp() > PUSH_TIMEOUT){
                    pr.getPromise().tryFailure(new PushException(ERROR_TIMEOUT, "push timeout"));
                    continue;
                }
                prs.add(pr);
                personIds.add(pr.getPersonId());
            }

            Map<PersonId, Set<String>> perid2lcsid = lcsFinder.getOnlinePersons(new ArrayList<>(personIds));

            for(PushRequest pr : prs){
                Set<String> lcsIds = perid2lcsid.get(pr.getPersonId());
                if(null == lcsIds || lcsIds.isEmpty()){
//                    pr.getPromise().tryFailure(new PushException(ERROR_OFFLINE, "user client offline"));
                    pr.getPromise().trySuccess(Collections.emptyList());
                    continue;
                }
                PushResponse presp = new PushResponse(lcsIds.size());
                for (String lcsId : lcsIds) {
                    pushToLcs(pr, presp, lcsId);
                }
            }
        }
    }

    private void pushToLcs(PushRequest req, PushResponse resp, String lcsId) {
        boolean success = false;
        for (int i = 0; i < 3; ++i) {
            TcpClient client = lcsFinder.getLcsClient(lcsId);
            if (null == client) {
                log.debug("user [{}] client offline. cant't get lcs client by lcsId:{}", req.getPersonId(), lcsId);
                boolean finish = resp.addData(null);
                if(finish) {
                    req.getPromise().trySuccess(resp.getList());
                }
                return;
            }
            if (!client.isConnected()) {
                lcsFinder.lcsClientInvalid(lcsId, client);
                continue;
            }
            Frame frame = new Frame();
            frame.setCmd(Frame.CMD_DATA);
            frame.setSeqId(Frame.newSeqId());
            frame.setBody(req.getData());

            log.debug("pushToLcs. lcsId:{} userId:{}, companyId:{}", lcsId, req.getPersonId().getUserId(), req.getPersonId().getCompanyId());

            client.send(frame, TimeUnit.MILLISECONDS, PUSH_TIMEOUT).addListener(new GenericFutureListener<Future<Frame>>() {
                @Override
                public void operationComplete(Future<Frame> future) throws Exception {
                    if (!future.isSuccess()) {
                        req.getPromise().tryFailure(future.cause());
                        return;
                    }
                    Frame fres = future.get();
                    boolean success = resp.addData(fres.getBody());
                    if(success) {
                        req.getPromise().trySuccess(resp.getList());
                    }
                }
            });

            success = true;
            break;
        }

        if (!success) {
            log.info("user [{}] client offline. cant't get useable lcs client by lcsId:{}", req.getPersonId(), lcsId);

            boolean finish = resp.addData(null);
            if(finish) {
                req.getPromise().trySuccess(resp.getList());
            }
//            req.getPromise().tryFailure(new PushException(ERROR_OFFLINE, errMsg));
        }

    }

    public int pushQueueSize(){
        return pushQueue.size();
    }

    public int executorQueueSize(){
        return executor.getQueue().size();
    }

}
