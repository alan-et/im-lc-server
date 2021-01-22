package com.alanpoi.im.lcs.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alanpoi.im.lcs.imsignal.SignalHandler;
import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.metrics.Counters;
import com.alanpoi.im.lcs.secprotocol.Cmd;
import com.alanpoi.im.lcs.secprotocol.SecpChannelManager;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannel;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannelAttrs;
import com.alanpoi.im.lcs.util.IPUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * websocket 服务处理类
 * @author zhuoxun.peng
 * @since 2019-4-12
 */
@Component
@ChannelHandler.Sharable
public class ServerChannelHandler extends SimpleChannelInboundHandler<Object> {


    private static final Logger log = LoggerFactory.getLogger(ServerChannelHandler.class);

    private static final String URI = "websocket";

    private WebSocketServerHandshaker handshaker ;


    @Autowired
    private SecpChannelManager channelMgr;
    @Autowired
    private SignalHandler sigHandler;


    @Autowired
    private Counters counters;

    /**
     * 连接异常   需要关闭相关资源
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("【系统异常】======>"+cause.toString());
        ctx.close();
        ctx.channel().close();
    }

    /**
     * 活跃的通道  也可以当作用户连接上客户端进行使用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        Channel chnl = ctx.channel();
        InetSocketAddress addr = (InetSocketAddress) chnl.remoteAddress();
        if (!IPUtil.isAliHealthIp(addr.getAddress().getAddress())) {
            log.info("【channelActive】=====>" + ctx.channel());
        }

        WSSecpChannel secpChnl = new WSSecpChannel(chnl);
        chnl.attr(SecpChannelAttrs.SECPCHANNEL).set(secpChnl);
        chnl.attr(SecpChannelAttrs.CLOSREASON).set("by remote");
        channelMgr.used(secpChnl);

        if (!IPUtil.isAliHealthIp(addr.getAddress().getAddress())) {
            log.info("socket channel created channelId: {}, ip:{}", secpChnl.getId(), addr.getHostName());
            counters.increment(Counters.SECP_WEBSOCKET_CONNECTED);
        }

    }

    /**
     * 不活跃的通道  就说明用户失去连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel chnl = ctx.channel();
        InetSocketAddress addr = (InetSocketAddress)chnl.remoteAddress();
        SecpChannel secpChnl = chnl.attr(SecpChannelAttrs.SECPCHANNEL).get();
        long chnlId = 0;
        long sessionId = 0;
        if(null != secpChnl){
            chnlId = secpChnl.getId();
            channelMgr.closed(secpChnl);
            sigHandler.channelClosed(secpChnl);
        }
        String reason = chnl.attr(SecpChannelAttrs.CLOSREASON).get();
        if(!IPUtil.isAliHealthIp(addr.getAddress().getAddress())) {
            log.info("channel closed. sessionId:{}, channelId:{}, ip:{}, reason:{}", sessionId, chnlId, addr.getHostName(), reason);
            counters.increment(Counters.SECP_WEBSOCKET_CLOSED);
        }

    }

    /**
     * 这里只要完成 flush
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 这里是保持服务器与客户端长连接  进行心跳检测 避免连接断开
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            PingWebSocketFrame ping = new PingWebSocketFrame();
            switch (stateEvent.state()){
                //读空闲（服务器端）
                case READER_IDLE:
                    log.info("【"+ctx.channel().remoteAddress()+"】读空闲（服务器端）");
                    SecpMessage message=new SecpMessage();
                    message.setCmd(Cmd.HEARTBEAT_REQ);
                    message.setBody("ping".getBytes());
                    ctx.channel().write(message);
                    //ctx.writeAndFlush(message);
                    break;
                //写空闲（客户端）
                case WRITER_IDLE:
                    log.info("【"+ctx.channel().remoteAddress()+"】写空闲（客户端）");
                    SecpMessage message2=new SecpMessage();
                    message2.setCmd(Cmd.HEARTBEAT_REQ);
                    message2.setBody("ping".getBytes());
                    ctx.channel().write(message2);
                    //ctx.writeAndFlush(ping);
                    break;
                case ALL_IDLE:
                    log.info("【"+ctx.channel().remoteAddress()+"】读写空闲");
                    break;
            }
        }
    }

    /**
     * 收发消息处理
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel chnl = ctx.channel();
        SecpChannel secpChnl = chnl.attr(SecpChannelAttrs.SECPCHANNEL).get();

        if(msg instanceof HttpRequest){
            doHandlerHttpRequest(ctx,(FullHttpRequest) msg);
        }else if(msg instanceof SecpMessage){
            doHandlerSecpMessage(secpChnl,(SecpMessage) msg);
        }else if(msg instanceof SignalProto.SignalRequest){

            SecpMessage secpMessage=new SecpMessage();
            secpMessage.setBody(((SignalProto.SignalRequest)msg).toByteArray());
            recvSignal(secpChnl, secpMessage);
        }else if(msg instanceof  PingWebSocketFrame){
            secpChnl.setTimeStamp(System.currentTimeMillis());
            SecpMessage message=new SecpMessage();
            message.setCmd(Cmd.HEARTBEAT_REQ);
            message.setBody("pong".getBytes());
            ctx.channel().write(message);
        }
    }

    private void doHandlerSecpMessage(SecpChannel secpChnl, SecpMessage msg){

        if(Cmd.CREATE_SECKEY_REQ == msg.getCmd()){
            return;
        }
        if(!(msg.getLcId() > 0 && msg.getLcId() == secpChnl.getId())){
        }
//        log.info("recv msg cmd:{}", msg.getCmd());
        switch(msg.getCmd()){
            case Cmd.TOS_SIGNAL_REQ:
                recvSignal(secpChnl, msg);
                break;
        }
    }

    /**
     * websocket消息处理
     * @param ctx
     * @param msg
     */
    private void doHandlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame msg) {
        //判断msg 是哪一种类型  分别做出不同的反应
        if(msg instanceof CloseWebSocketFrame){
            log.info("【关闭】");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg);
            return ;
        }
        if(msg instanceof PingWebSocketFrame){
            log.info("【ping】");
            PongWebSocketFrame pong = new PongWebSocketFrame(msg.content().retain());
            ctx.channel().writeAndFlush(pong);
            return ;
        }
        if(msg instanceof PongWebSocketFrame){
            log.info("【pong】");
            PingWebSocketFrame ping = new PingWebSocketFrame(msg.content().retain());
            ctx.channel().writeAndFlush(ping);
            return ;
        }
        if(!(msg instanceof TextWebSocketFrame)){
            log.info("【不支持二进制】");
            throw new UnsupportedOperationException("不支持二进制");
        }
        String text=((TextWebSocketFrame) msg).text();
        JSONObject json= JSON.parseObject(text);

    }


    /**
     * wetsocket第一次连接握手
     * @param ctx
     * @param msg
     */
    private void doHandlerHttpRequest(ChannelHandlerContext ctx, FullHttpRequest msg) {
        if(msg.uri().startsWith("/health")){
            sendHttpResponse(ctx, msg, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("ok".getBytes())));
            return;
        }
        //SignalHttpApi signalHttpApi=new SignalHttpApi();
       // FullHttpResponse response = signalHttpApi.recv(msg);
        // http 解码失败
        if(!msg.decoderResult().isSuccess() || (!"websocket".equals(msg.headers().get("Upgrade")))){
            sendHttpResponse(ctx, msg,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //可以获取msg的uri来判断
        String uri = msg.uri();
        if(!uri.substring(1).equals(URI)){
            ctx.close();
        }
        ctx.attr(AttributeKey.valueOf("type")).set(uri);
        //可以通过url获取其他参数
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                "ws://"+msg.headers().get("Host")+"/"+URI+"",null,true
        );
        handshaker = factory.newHandshaker(msg);
        if(handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
//            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        }
        //进行连接
        handshaker.handshake(ctx.channel(), (FullHttpRequest) msg);
        //可以做其他处理
    }

    //接收信令
    private void recvSignal(SecpChannel secpChnl, SecpMessage msg){
        sigHandler.recvSignal(secpChnl, msg);
    }
    //接收心跳
    private void recvHeartbeat(SecpChannel secpChnl, SecpMessage msg) throws Exception{
        log.info("recv heartbeat");
        secpChnl.write(msg);
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        int contentLength = 0;
        if (res.content() != null) {
            contentLength = res.content().readableBytes();
        }
        res.headers().add(HttpHeaderNames.CONTENT_LENGTH, contentLength);
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}