package com.alanpoi.im.lcs.websocket;

import com.alanpoi.im.lcs.websocket.handler.ServerChannelHandler;
import com.alanpoi.im.lcs.websocket.handler.WebsocketDecoder;
import com.alanpoi.im.lcs.websocket.handler.WebsocketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * websocket 服务
 * @author zhuoxun.peng
 * @since 2019-4-12
 */
@Component
public class WebsocketServer {

    private static final Logger log = LoggerFactory.getLogger(WebsocketServer.class);

    @Value("${server.ws.port}")
    private int port;
    @Autowired
    private ServerChannelHandler serverChannelHandler;

    private void startServer() {
        //服务端需要2个线程组  boss处理客户端连接  work进行客服端连接之后的处理
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            //服务器 配置
            bootstrap.group(boss, work).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        private DefaultEventExecutorGroup executor = new DefaultEventExecutorGroup(16, new DefaultThreadFactory("entryMessage"));

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // HttpServerCodec：将请求和应答消息解码为HTTP消息

                            socketChannel.pipeline().addLast("http-codec", new HttpServerCodec());
                            // HttpObjectAggregator：将HTTP消息的多个部分合成一条完整的HTTP消息
                            socketChannel.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                            // 主要用于处理大数据流，比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的; 增加之后就不用考虑这个问题了
                            // ChunkedWriteHandler：向客户端发送HTML5文件
                            socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            // WebSocket数据压缩
                            socketChannel.pipeline().addLast(new WebSocketServerCompressionHandler());
                            // 协议包长度限制
                            //socketChannel.pipeline().addLast(new WebSocketServerProtocolHandler("/ws", null, true));
                            // 协议包解码
                            socketChannel.pipeline().addLast(new WebsocketDecoder());
                            // 协议包编码
                            socketChannel.pipeline().addLast(new WebsocketEncoder());
                             // 协议包解码时指定Protobuf字节数实例化为CommonProtocol类型
                            //socketChannel.pipeline().addLast(new ProtobufDecoder(SignalProto.SignalRequest.getDefaultInstance()));
                            // 进行设置心跳检测
                            //socketChannel.pipeline().addLast(new IdleStateHandler(60, 30, 60 * 30, TimeUnit.SECONDS));
                            // 配置通道处理  来进行业务处理
                            socketChannel.pipeline().addLast(executor,serverChannelHandler);
                        }
                    }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true);
            //绑定端口  开启事件驱动
            log.info("【Websocket服务器启动成功========端口：" + port + "】");
            Channel channel = bootstrap.bind(port).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭资源
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    @PostConstruct()
    public void init() {
        //需要开启一个新的线程来执行netty server 服务器
        new Thread(new Runnable() {
            @Override
            public void run() {
                startServer();
            }
        }).start();
    }
}
