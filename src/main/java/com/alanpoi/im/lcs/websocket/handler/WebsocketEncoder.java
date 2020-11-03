package com.alanpoi.im.lcs.websocket.handler;

import com.alanpoi.im.lcs.secprotocol.Cmd;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;

/**
 * websocket protobuf 消息编码
 *
 * @author zhuoxun.peng
 */
public class WebsocketEncoder extends MessageToMessageEncoder<SecpMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, SecpMessage msg, List<Object> out) throws Exception {
        ByteBuf result = null;
        //心跳返回pong协议
        if (msg.getCmd() == Cmd.HEARTBEAT_REQ) {
            PongWebSocketFrame pong = new PongWebSocketFrame();
            pong.content().writeBytes(msg.getBody());
            out.add(pong);
        }
        result = Unpooled.wrappedBuffer(msg.getBody());
        WebSocketFrame frame = new BinaryWebSocketFrame();
        //frame.content().writeBytes(msg.getBody());
        frame.content().writeBytes(result);
        out.add(frame);
        //pkg.release();
        result.release();
    }
}
