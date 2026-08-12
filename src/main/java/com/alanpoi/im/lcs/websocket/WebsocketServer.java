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
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * websocket 服务
 * @author zhuoxun.peng
 * @since 2019-4-12
 */
@Component
public class WebsocketServer {

    /**
     * 心跳间隔(秒)。必须<b>小于链路上最小的那个空闲超时</b> ——
     * ALB 默认 60s、Nginx proxy_read_timeout 默认 60s,取 30s 留一倍余量。
     * 改大之前先确认 ALB 上的实际配置。
     */
    private static final int HEARTBEAT_IDLE_SECONDS = 30;

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
                            // 心跳:ALL_IDLE 30 秒(读写都空闲)触发,由服务端主动发 WebSocket Ping 帧。
                            //
                            // 浏览器的 JS 没有发送 Ping 帧的 API,只能用 send()
                            // 发应用层心跳;而后台标签页的定时器会被 Chrome 压到约 1 分钟一次,
                            // 心跳还没发出去连接就被中间设备(ALB 默认空闲 60s)踢了。
                            // 反过来由服务端发 Ping,浏览器在网络层自动回 Pong,不经过 JS,不受节流影响。
                            //
                            // 放在解码器之前:这样读写计数看到的是原始帧,不受 WebsocketDecoder
                            // 过滤规则的影响(它只放行 Ping/Pong/Binary)。
                            //
                            // 只开 ALL_IDLE、不开 READER_IDLE 的关闭逻辑:目前没有任何空闲超时,
                            // 贸然加"读空闲即关闭"可能误杀存活但安静的 iOS/PC 连接。保活是本次目标,
                            // 死连接检测是另一件事,要做再单独评估。
                            socketChannel.pipeline().addLast(
                                    new IdleStateHandler(0, 0, HEARTBEAT_IDLE_SECONDS, TimeUnit.SECONDS));
                            // 协议包解码
                            socketChannel.pipeline().addLast(new WebsocketDecoder());
                            // 协议包编码
                            socketChannel.pipeline().addLast(new WebsocketEncoder());
                             // 协议包解码时指定Protobuf字节数实例化为CommonProtocol类型
                            //socketChannel.pipeline().addLast(new ProtobufDecoder(SignalProto.SignalRequest.getDefaultInstance()));
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
