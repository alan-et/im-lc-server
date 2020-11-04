package com.alanpoi.im.lcs.transtools;

import com.alanpoi.im.lcs.transtools.network.Frame;
import com.alanpoi.im.lcs.transtools.network.TcpServer;
import com.alanpoi.im.lcs.transtools.redis.RedisLcsRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetSocketAddress;

public class TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServer.class);


    public static void main(String[] args) {
        TcpServer server = new TcpServer(2, new ServerHandler());
        InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 7002);
        try {
            server.start(localAddr);
        } catch (Exception e) {
            log.error("", e);
            return;
        }

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(2);
        config.setMaxWaitMillis(3000);
        config.setMaxIdle(2);
        config.setMinIdle(2);

        JedisPool jedisPool =  new JedisPool(config, "172.16.18.36", 19001, 1000, "qzdim");

        RedisLcsRegistry registry = new RedisLcsRegistry(2, jedisPool);
        registry.init(localAddr);



        registry.registerUser("2", "2");
    }

    @ChannelHandler.Sharable
    private static class ServerHandler extends SimpleChannelInboundHandler<Frame> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
            Channel channel = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();

            if (Frame.CMD_HEARTBEAT == msg.getCmd()) {
                log.info("recv heartbeat from:{}", addr.toString());
            } else if (Frame.CMD_DATA == msg.getCmd()) {

                String data = new String(msg.getBody());
                log.info("recv data from:{}, data:{}", addr.toString(), data);

                data = data + "---res";
                Frame res = msg.clone();
                res.setCmd(Frame.CMD_DATA_RES);
                res.setBody(data.getBytes());
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
