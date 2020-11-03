package com.alanpoi.im.lcs.transtools.network;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * 使用Frame协议的TCP Client
 */
public class TcpClient {

    public static final AttributeKey<TcpClient>  CLIENT = AttributeKey.valueOf("TcpClient");
    public static final AttributeKey<ScheduledFuture<?>> SCHEDULE = AttributeKey.valueOf("schedule");

    private volatile Channel channel;
    private EventExecutor executor;

    private Map<Integer, Promise<Frame>> waitingPromises = new ConcurrentHashMap<>();

    private Map<String, Object> attrs = new ConcurrentHashMap<>();

    public TcpClient(Channel channel, EventExecutor executor){
        this.channel = channel;
        this.executor = executor;
    }

    public void setAttr(String name, String value){
        attrs.put(name, value);
    }
    public Object getAttr(String name){
        return attrs.get(name);
    }

    public boolean isConnected(){
        Channel chnl = channel;
        return null != chnl && chnl.isActive();
    }

    public boolean send(Frame frame){
        Channel chnl = channel;
        if(null == chnl){
            return false;
        }

        if(!chnl.isActive()){
            return false;
        }
        chnl.writeAndFlush(frame);
        return true;
    }

    public boolean sendHeartbeat(){
        Frame hb = new Frame();
        hb.setCmd(Frame.CMD_HEARTBEAT);
        hb.setSeqId(Frame.newSeqId());
        return send(hb);
    }

    public Promise<Frame> send(Frame frame, TimeUnit timeUnit, long timeout){
        Channel chnl = channel;

        Promise<Frame> res = new DefaultPromise<>(executor);
        if(null == chnl){
            res.tryFailure(new Exception("channel is null"));
            return res;
        }

        if(!chnl.isActive()){
            res.tryFailure(new Exception("chanell is inactive"));
            return res;
        }

        waitResponse(frame, res, timeUnit, timeout);
        chnl.writeAndFlush(frame);
        return res;
    }

    public void close(){
        Channel chnl = channel;
        if(null == chnl) return;
        channel = null;

        chnl.close();

        onInactive();
    }

    public void onInactive(){
        Channel chnl = channel;
        if(null == chnl){
            return;
        }
        channel = null;
        ScheduledFuture<?> future = chnl.attr(SCHEDULE).getAndSet(null);
        if(null != future){
            future.cancel(true);
        }
        chnl.attr(CLIENT).set(null);
    }


    protected void waitResponse(Frame frame, Promise<Frame> res, TimeUnit timeUnit, long timeout){
        ScheduledFuture<?> future = executor.schedule(new Runnable() {
            @Override
            public void run() {
                Promise<Frame> wres = waitingPromises.remove(frame.getSeqId());
                if(null == wres){
                    return;
                }
                if(wres.isDone()){
                   return;
                }
                wres.tryFailure(new Exception("response timeout"));
            }
        }, timeout, timeUnit);
        waitingPromises.put(frame.getSeqId(), res);
    }

    public void onResponse(Frame frame){
        Promise<Frame> res = waitingPromises.remove(frame.getSeqId());
        if(null == res){
            return;
        }
        if(null == frame){
            res.tryFailure(new Exception("response frame is null"));
            return;
        }

        res.trySuccess(frame);
    }

}
