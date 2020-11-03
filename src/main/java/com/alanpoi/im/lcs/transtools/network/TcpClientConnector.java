package com.alanpoi.im.lcs.transtools.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * Tcp Client连接器
 */
public class TcpClientConnector {
    private Bootstrap bootstrap;

    private EventLoopGroup workerElg;

    private List<ChannelHandler> handlers = new LinkedList<>();

    public TcpClientConnector(int workerThreads){
        workerElg = new NioEventLoopGroup(workerThreads);

        bootstrap = new Bootstrap();
        bootstrap.group(workerElg)
            .channel(NioSocketChannel.class)
            .handler(new HandlerInitalizer())
        ;

    }

    public void addHandler(ChannelHandler handler){
        synchronized (handlers){
            handlers.add(handler);
        }
    }

    public TcpClient connect(InetSocketAddress address) throws Exception{
        ChannelFuture future = bootstrap.connect(address).sync();
        Channel channel = future.channel();

        TcpClient client = new TcpClient(channel, workerElg.next());
        channel.attr(TcpClient.CLIENT).set(client);

        ScheduledFuture<?> schedule = workerElg.next().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                client.sendHeartbeat();
            }
        }, 0, Constants.HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
        channel.attr(TcpClient.SCHEDULE).set(schedule);

        return client;
    }

    private class HandlerInitalizer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            // TODO Auto-generated method stub
            //log.info("initChannel");

            ch.pipeline().addLast(new FrameEncoder());
            ch.pipeline().addLast(new FrameDecoder());
            ch.pipeline().addLast(new ReadTimeoutHandler(Constants.READ_TIMEOUT, TimeUnit.SECONDS));
            ch.pipeline().addLast(new TcpClientHandler());
            synchronized (handlers) {
                for (ChannelHandler handler : handlers) {
                    ch.pipeline().addLast(handler);
                }
            }

        }

    }
}
