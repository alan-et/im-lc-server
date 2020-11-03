package com.alanpoi.im.lcs.secprotocol;

import com.alanpoi.im.lcs.CurrentTime;
import com.alanpoi.im.lcs.metrics.Gauges;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannel;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannelStatus;
import com.alanpoi.im.lcs.websocket.handler.WSSecpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecpChannelManager {
    private static final Logger log = LoggerFactory.getLogger(SecpChannelManager.class);

    private static final int CHANNEL_TIMEOUT = 30000; //新channel超时时间
    public static final int HEARTBEAT_TIMEOUT = 12 * 60 * 1000; //session心跳超时时间

    private volatile boolean isStart = false;
    private Thread thrWorker = null;

    //新的socketChannel
    private ConcurrentHashMap<Long, SecpChannel> newChannels = new ConcurrentHashMap<>();
    //正在正常使用的sockerChannel
    private ConcurrentHashMap<Long, SecpChannel> usedhannels = new ConcurrentHashMap<>();
    //超时队列


    @Autowired
    private Gauges gauges;


    @PostConstruct
    public void start(){
        if(isStart) return;
        isStart = true;

        thrWorker = new Thread(new Worker());
        thrWorker.start();
    }
    @PreDestroy
    public void stop(){
        if(!isStart) return;
        isStart = false;
        thrWorker.interrupt();
        try {
            thrWorker.join(10000);
        } catch (InterruptedException e) {
        }
    }

    //添加新的channel
    public void add(SecpChannel secpChnl){
        newChannels.put(secpChnl.getId(), secpChnl);
    }
    //设置正常使用的channel
    public void used(SecpChannel secpChnl){
        secpChnl.setStatus(SecpChannelStatus.STATUS_USED);
        newChannels.remove(secpChnl.getId());
        usedhannels.put(secpChnl.getId(), secpChnl);
    }
    //得到channel
    public SecpChannel get(long chnlId){
        return usedhannels.get(chnlId);
    }

    //channel断开
    public void closed(SecpChannel chnl){
        //log.info("remove channle {}", chnl.getId());
        newChannels.remove(chnl.getId());
        usedhannels.remove(chnl.getId());
    }

    //清理没有用的channel
    private void clearInvalidChannel(){
        List<Long> newChnlTimeout = new ArrayList<Long>();
        List<Long> usedChnlTimeout = new ArrayList<Long>();
        List<Long> chnlClosed = new ArrayList<Long>();

        long now = CurrentTime.getCurrentTime();

        for(Map.Entry<Long, SecpChannel> entry : newChannels.entrySet()){
            Long chnlId = entry.getKey();
            SecpChannel chnl = entry.getValue();

            if(now - CHANNEL_TIMEOUT > chnl.getTimeStamp()){
                newChnlTimeout.add(chnlId);
                continue;
            }
            if(chnl.getStatus().getVal() == SecpChannelStatus.STATUS_CLOSED.getVal()){
                chnlClosed.add(chnlId);
                continue;
            }
        }

        int LlCustomCount = 0;
        int LlWebsocketCount = 0;
        for(Map.Entry<Long, SecpChannel> entry : usedhannels.entrySet()){
            Long chnlId = entry.getKey();
            SecpChannel chnl = entry.getValue();
            if(now - HEARTBEAT_TIMEOUT > chnl.getTimeStamp()){
                usedChnlTimeout.add(chnlId);
                continue;
            }

            if (chnl.getStatus().getVal() == SecpChannelStatus.STATUS_CLOSED.getVal()) {
                chnlClosed.add(chnlId);
                continue;
            }
            if (chnl instanceof DefaultSecpChannel) {
                LlCustomCount++;
            } else if (chnl instanceof WSSecpChannel) {
                LlWebsocketCount++;
            }

            /**
            if(chnl.getChnlProto() == OldSecpChannel.ChannelProtocol.CUSTOM){
                LlCustomCount ++;
            }else if(chnl.getChnlProto() == OldSecpChannel.ChannelProtocol.WEBSOCKET){
                LlWebsocketCount ++;
            }**/
        }


        for(Long chnlId : newChnlTimeout){
            SecpChannel chnl = newChannels.get(chnlId);
            if(null == chnl){
                continue;
            }
            closed(chnl);
            chnl.close("new channel timeout");
            log.info("new channel timeout. channelId:{}", chnl.getId());
        }


        for(Long chnlId : usedChnlTimeout){
            SecpChannel chnl = usedhannels.get(chnlId);
            if(null == chnl){
                continue;
            }
            closed(chnl);
            chnl.close("used channel timeout");
            log.info("used channel timeout. channelId:{}", chnl.getId());
        }

        for(Long chnlId : chnlClosed){
            SecpChannel chnl = usedhannels.get(chnlId);
            if(null == chnl){
                continue;
            }
            closed(chnl);
            chnl.close("channel was closed");
            log.info("channel was closed. channelId:{}", chnl.getId());
        }

        gauges.report(Gauges.SECP_CUSTOM_BUILDING_COUNT, newChannels.size());
        gauges.report(Gauges.SECP_CUSTOM_LLCOUNT, LlCustomCount);
        gauges.report(Gauges.SECP_WEBSOCKET_LLCOUNT, LlWebsocketCount);

    }

    public int getNewChannelCount(){
        return newChannels.size();
    }
    public int getUsedChannelCount(){
        return usedhannels.size();
    }

    private class Worker implements Runnable{

        @Override
        public void run() {
            while(isStart){
                clearInvalidChannel();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }

        }

    }

}
