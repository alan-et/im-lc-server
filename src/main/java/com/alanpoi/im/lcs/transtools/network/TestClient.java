package com.alanpoi.im.lcs.transtools.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class TestClient {
    private static final Logger log = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args){
        TcpClientConnector connector = new TcpClientConnector(2);
        connector.addHandler(new ClientHandler());

        InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", 7002);

        try {
            TcpClient client = connector.connect(serverAddr);

            Frame req = new Frame();
            req.setCmd(Frame.CMD_DATA);
            req.setSeqId(Frame.newSeqId());
            req.setBody("this is test req".getBytes());
            client.send(req, TimeUnit.SECONDS, 3).addListener(new GenericFutureListener<Future<Frame>>() {
                @Override
                public void operationComplete(Future<Frame> future) throws Exception {
                    if(!future.isSuccess()){
                        log.error("", future.cause());
                        return;
                    }
                    Frame res = future.get();
                    String str = new String(res.getBody());
                    log.info("recv res. data:{}", str);
                }
            });
            log.info("call send");

            Thread.sleep(60 * 1000);
        }catch (Exception e){
            log.error("", e);
        }


    }

    @ChannelHandler.Sharable
    private static class ClientHandler extends SimpleChannelInboundHandler<Frame> {

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, Frame msg) throws Exception {

            if(Frame.CMD_HEARTBEAT_RES == msg.getCmd()){
                log.info("recv heart res");
            }

            //ctx.fireChannelRead(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress)channel.localAddress();
            log.info("client {} closed", addr.toString());

            //ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress)channel.localAddress();
            log.info("client {} execption", addr.toString());
            log.error("", cause);

            //ctx.fireExceptionCaught(cause);
        }
    }
}
