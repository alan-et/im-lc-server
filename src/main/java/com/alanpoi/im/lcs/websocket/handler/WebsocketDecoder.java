package com.alanpoi.im.lcs.websocket.handler;

import com.alanpoi.im.lcs.imsignal.SignalProto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * websocket protobuf协议解码
 *
 * @author zhuoxun.peng
 */
public class WebsocketDecoder extends MessageToMessageDecoder<WebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(WebsocketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        if (frame instanceof PingWebSocketFrame || frame instanceof PongWebSocketFrame) {
            // Pong 必须放行:服务端空闲时会主动发 Ping,对端(浏览器在网络层自动、原生端由库自动)
            // 回的就是 Pong。这里若不放行,ServerChannelHandler 收不到,活跃时间无法刷新。
            out.add(frame);
            //传递给下个handler，由下个handler释放
            frame.retain();
        } else if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf in = ((BinaryWebSocketFrame) frame).content();
            byte[] bytes = new byte[in.readableBytes()];
            in.getBytes(in.readerIndex(), bytes);
            out.add(SignalProto.SignalRequest.parseFrom(bytes));
        }
    }
}
