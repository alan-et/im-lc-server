package com.alanpoi.im.lcs.server;

import com.qzd.im.common.util.NetworkUtil;
import com.alanpoi.im.lcs.imsignal.TransHandler;
import com.alanpoi.im.lcs.metrics.Counters;
import com.alanpoi.im.lcs.transtools.LcsRegistry;
import com.alanpoi.im.lcs.transtools.network.Frame;
import com.alanpoi.im.lcs.transtools.network.TcpServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * 接收推送的服务Server
 */
@Component
public class PushServer {
    private static final Logger log = LoggerFactory.getLogger(PushServer.class);

    @Value("${server.push.port}")
    private int port;

    @Autowired
    private TransHandler transHandler;

    @Autowired
    private LcsRegistry lcsResgistry;
    @Autowired
    private Counters counters;


    private TcpServer server;

    @PostConstruct
    public void start() throws Exception{
        server = new TcpServer(8, new FrameHandler());

        server.start(new InetSocketAddress(port));
        log.info("lcs push server start");

        lcsResgistry.init(new InetSocketAddress(NetworkUtil.getLocalIP(), port));
    }

    @PreDestroy
    public void stop(){
        lcsResgistry.destroy();

        server.shutdown();
    }

    @ChannelHandler.Sharable
    private class FrameHandler extends SimpleChannelInboundHandler<Frame>{
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
            Channel chnl = ctx.channel();
            InetSocketAddress addr = (InetSocketAddress)chnl.remoteAddress();
            if(Frame.CMD_HEARTBEAT == msg.getCmd()){
                log.info("recv heartbeat from [{}]", addr.toString());
                return;
            }
            if(Frame.CMD_DATA == msg.getCmd()){
                counters.increment(Counters.PUSH_SIGNAL_TOTAL);
                transHandler.push(chnl, msg);
            }
        }
    }

}
