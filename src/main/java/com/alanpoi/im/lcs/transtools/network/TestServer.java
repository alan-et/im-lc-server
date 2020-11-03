package com.alanpoi.im.lcs.transtools.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TestServer {
    private static final Logger log = LoggerFactory.getLogger(TestServer.class);


    public static void main(String[] args) {
        TcpServer server = new TcpServer(2, new ServerHandler());
        try {
            server.start(new InetSocketAddress(7002));
        } catch (Exception e) {
            log.error("", e);
        }

    }

    @ChannelHandler.Sharable
    private static class ServerHandler extends SimpleChannelInboundHandler<Frame> {
        @Override
        protected void messageReceived(ChannelHandlerContext ctx, Frame msg) throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();

            if (Frame.CMD_HEARTBEAT == msg.getCmd()) {
                log.info("recv heartbeat from:{}", addr.toString());
            } else if (Frame.CMD_DATA == msg.getCmd()) {
                String str = new String(msg.getBody());
                log.info("recv data from:{}. data:{}", addr.toString(), str);
                Frame res = msg.clone();
                res.setCmd(Frame.CMD_DATA_RES);
                str = str + "----res";
                res.setBody(str.getBytes());
                res.setCode(111);
                channel.writeAndFlush(res);
            }

            //ctx.fireChannelRead(msg);
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
            log.info("client {} connected", addr.toString());
            //ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
            log.info("client {} closed", addr.toString());

            //ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
            log.info("client {} execption", addr.toString());
            log.error("", cause);

            //ctx.fireExceptionCaught(cause);
        }

    }


}
