package com.alanpoi.im.lcs.transtools;

import com.qzd.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.network.Frame;
import com.alanpoi.im.lcs.transtools.network.TcpClientConnector;
import com.alanpoi.im.lcs.transtools.network.TcpClientLogHandler;
import com.alanpoi.im.lcs.transtools.redis.RedisLcsFinder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetSocketAddress;
import java.util.List;

public class TestClient {

    private static final Logger log = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args){
        TcpClientConnector connector = new TcpClientConnector(2);
//        connector.addHandler(new ClientHandler());
        connector.addHandler(new TcpClientLogHandler());

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(2);
        config.setMaxWaitMillis(3000);
        config.setMaxIdle(2);
        config.setMinIdle(2);
        JedisPool jedisPool =  new JedisPool(config, "172.16.18.36", 19001, 1000, "qzdim");

        RedisLcsFinder lcsFinder = new RedisLcsFinder(connector, jedisPool);

        LcsPusher pusher = new LcsPusher(2, lcsFinder);

        PersonId personId = new PersonId("2", "2");

        //SignalProto.SignalTranspond sig = buildSignalTranspond(personId, "this is test sig");
        FutureListener listener = new FutureListener();

        String data = "this is test sig to person:"+personId.toString();
        pusher.push(personId, data.getBytes()).addListener(listener);

        try{
            Thread.sleep(12);
        }catch (Exception e){

        }

        personId = new PersonId(null, "2");
        data = "this is test sig to user:"+personId.getUserId();
        pusher.push(personId, data.getBytes()).addListener(listener);


    }

    private static class FutureListener implements GenericFutureListener<Future<List<byte[]>>> {
        @Override
        public void operationComplete(Future<List<byte[]>> future) throws Exception {
            if(!future.isSuccess()){
                log.error("", future.cause());
                return;
            }
            List<byte[]> list = future.get();
            for (byte[] bytes : list) {
                if(bytes==null) continue;
                String str = new String(bytes);
                log.info("recv res: {}", str);
            }
        }
    }

    /*
    private static SignalProto.SignalTranspond buildSignalTranspond(PersonId personId, String data) {
        SignalProto.SignalTranspond.Target.Builder targetBuilder = SignalProto.SignalTranspond.Target.newBuilder();
        targetBuilder.addClientType(1);
        targetBuilder.setUserId(personId.getUserId());
        targetBuilder.setCompanyId(personId.getCompanyId());


        SignalProto.SignalTranspond.Target target = targetBuilder.build();
        SignalProto.SignalTranspond.Header header = SignalProto.SignalTranspond.Header.newBuilder().
                setCmd(101).
                setVersion(1).
                addTargets(target).build();
        return SignalProto.SignalTranspond.newBuilder()
                .setHeader(header)
                .setBody(ByteString.copyFrom(data.getBytes()))
                .build();
    }*/

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
