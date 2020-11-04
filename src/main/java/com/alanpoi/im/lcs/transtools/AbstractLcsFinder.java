package com.alanpoi.im.lcs.transtools;

import com.alanpoi.im.lcs.transtools.network.Frame;
import com.alanpoi.im.lcs.transtools.network.TcpClient;
import com.alanpoi.im.lcs.transtools.network.TcpClientConnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS服务finder类型的抽象实现
 */

public abstract class AbstractLcsFinder implements LcsFinder{
    private static final Logger log = LoggerFactory.getLogger(AbstractLcsFinder.class);

    private static final int RETYRCONN_INTERVAL = 5000; //断开重连时间间隔(ms)

    //LcsContext状态
    private static final int LCSCTX_CONNECTING = 1; //正在连接
    private static final int LCSCTX_USEABLE = 2; //可用
    private static final int LCSCTX_CLOSED = 3;     //连接断开


    ////////////////////////////////


    private static final String ATTR_LCSID = "lcsId";

    private ConcurrentHashMap<String, LcsContext> lcsCtxs = new ConcurrentHashMap<>();
    private TcpClientConnector connector;

    public AbstractLcsFinder(TcpClientConnector connector){
        this.connector = connector;

        connector.addHandler(new LcsClientHandler());
    }

    @Override
    public TcpClient getLcsClient(String lcsId){
        long begin = System.currentTimeMillis();
        LcsContext ctx = lcsCtxs.get(lcsId);

        if(null != ctx){
            if(ctx.getStatus() == LCSCTX_USEABLE){
                return ctx.getClient();
            }else if(ctx.getStatus() == LCSCTX_CLOSED){
                long curt = System.currentTimeMillis();
                if(curt - ctx.getStatusTime() < RETYRCONN_INTERVAL){
                    return null;
                }else{
                    TcpClient cli = ctx.getClient();
                    if(null != cli) cli.close();
                    ctx.setClient(null);

                    ctx.setStatus(LCSCTX_CONNECTING);
                }
            }
        }else{
            ctx = new LcsContext();
            ctx.setLcsId(lcsId);
            LcsContext preCtx = lcsCtxs.putIfAbsent(lcsId, ctx);
            if(null != preCtx){
                ctx = preCtx;
                //log.info("use old new lcsCtx");
            }else{
                //log.info("create new lcsCtx");
            }
        }

        InetSocketAddress address = getLcsAddress(lcsId);
        if(null == address){
            lcsCtxs.remove(lcsId);
            return null;
        }

        synchronized (ctx){
            if(ctx.getStatus() == LCSCTX_USEABLE){
                return ctx.getClient();
            }
            if(ctx.getStatus() == LCSCTX_CLOSED){
                return null;
            }

            try {
                ctx.setStatus(LCSCTX_CONNECTING);

                TcpClient client = connector.connect(address);
                client.setAttr(ATTR_LCSID, lcsId);

                ctx.setLcsId(lcsId);
                ctx.setAddress(address);
                ctx.setClient(client);
                ctx.setStatus(LCSCTX_USEABLE);

                long cost = System.currentTimeMillis() - begin;
                log.info("connect lcs:{} lcsId:{} successful cost:{}", address.toString(), lcsId, cost);

                return client;
            }catch (Exception e){
                log.error("connect lcs {} error:", address.toString());
                log.error("", e);
                ctx.setStatus(LCSCTX_CLOSED);
                return null;
            }

        }

    }

    @Override
    public void lcsClientInvalid(String lcsId, TcpClient client) {
        if(null == client) return;

        LcsContext ctx = lcsCtxs.get(lcsId);
        if(null == ctx) return;

        TcpClient curClient = ctx.getClient();
        if(null == curClient) return;

        if(curClient.equals(client)){
            lcsCtxs.remove(lcsId);
            curClient.close();
        }
    }

    private InetSocketAddress getLcsAddress(String lcsId) {
        LcsInfo lcsInfo = getLcsInfo(lcsId);
        if (lcsInfo != null) {
            return new InetSocketAddress(lcsInfo.getIp(), lcsInfo.getPort());
        }
        return null;
    };

    private void onLcsClientClosed(TcpClient client){
        if(null == client){
            return;
        }
        String lcsId = (String)client.getAttr(ATTR_LCSID);
        lcsCtxs.remove(lcsId);
    }

    @ChannelHandler.Sharable
    private class LcsClientHandler extends SimpleChannelInboundHandler<Frame> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
            if(Frame.CMD_HEARTBEAT_RES == msg.getCmd()){
                log.info("recv heartbeat:[{}]", ctx.channel().toString());
            }
            ctx.fireChannelRead(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel chnl = ctx.channel();

            InetSocketAddress addr = (InetSocketAddress)chnl.remoteAddress();
            log.info("lcs client closed. lcs:{}", addr.toString());

            TcpClient client = chnl.attr(TcpClient.CLIENT).get();
            if(null != client){
                onLcsClientClosed(client);
            }

            ctx.fireChannelInactive();
        }
    }

    private class LcsContext{
        private String lcsId;
        private InetSocketAddress address;
        private volatile TcpClient client;

        private volatile int status;
        private volatile long statusTime;

        public LcsContext(){
            status = LCSCTX_CONNECTING;
            statusTime = System.currentTimeMillis();
        }

        public String getLcsId() {
            return lcsId;
        }

        public void setLcsId(String lcsId) {
            this.lcsId = lcsId;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public void setAddress(InetSocketAddress address) {
            this.address = address;
        }

        public TcpClient getClient() {
            return client;
        }

        public void setClient(TcpClient client) {
            this.client = client;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
            this.statusTime = System.currentTimeMillis();
        }
        public long getStatusTime(){
            return statusTime;
        }

    }


}
