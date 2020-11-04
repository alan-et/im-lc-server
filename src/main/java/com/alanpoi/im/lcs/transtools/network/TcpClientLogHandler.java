package com.alanpoi.im.lcs.transtools.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class TcpClientLogHandler  extends SimpleChannelInboundHandler<Frame> {
    private static Logger log = LoggerFactory.getLogger(TcpClientLogHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        if(Frame.CMD_HEARTBEAT_RES == msg.getCmd()){
            log.info("recv heart res {}", ctx.channel());
        }

        //ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("client {} closed", channel);

        //ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        Channel channel = ctx.channel();
        log.info("client {} execption", channel);
        log.error("", cause);
    }
}
