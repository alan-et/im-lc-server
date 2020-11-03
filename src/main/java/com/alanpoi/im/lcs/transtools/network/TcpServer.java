package com.alanpoi.im.lcs.transtools.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * 使用Frame协议的TCP Server
 */
public class TcpServer {
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private ServerBootstrap bootstrap;
    private EventLoopGroup bossElg;
    private EventLoopGroup workerElg;
    private ChannelHandler inbHandler;

    private Channel serverChannel;

    public TcpServer(int workerThreads, ChannelHandler inboundHandler){
        bossElg = new NioEventLoopGroup();
        workerElg = new NioEventLoopGroup(workerThreads);
        inbHandler = inboundHandler;
    }

    public void start(InetSocketAddress address) throws Exception{
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossElg, workerElg)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HandlerInitalizer())
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
            //.childOption(ChannelOption.SO_KEEPALIVE, true)
            ;

            ChannelFuture future = bootstrap.bind(address).sync();
            serverChannel = future.channel();

            log.info("frame Tcp server listen at:{}", address.toString());
        }catch (Exception e){
            log.error("start frame Tcp server at:{} error", address.toString());
            throw e;
        }

    }
    public void shutdown(){
        serverChannel.close();
        bossElg.shutdownGracefully();
        workerElg.shutdownGracefully();
    }

    private class HandlerInitalizer extends ChannelInitializer<SocketChannel> {

        private DefaultEventExecutorGroup executor = null;

        public HandlerInitalizer(){
            executor = new DefaultEventExecutorGroup(16, new DefaultThreadFactory("secp"));
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            // TODO Auto-generated method stub
            //log.info("initChannel");

            ch.pipeline().addLast(new FrameEncoder());
            ch.pipeline().addLast(new FrameDecoder());
            ch.pipeline().addLast(new ReadTimeoutHandler(Constants.READ_TIMEOUT, TimeUnit.SECONDS));
            ch.pipeline().addLast(new TcpServerHandler());
            ch.pipeline().addLast(inbHandler);
        }

    }
}
