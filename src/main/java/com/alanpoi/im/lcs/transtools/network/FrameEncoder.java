package com.alanpoi.im.lcs.transtools.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameEncoder extends MessageToByteEncoder<Frame> {
    private static final Logger log = LoggerFactory.getLogger(FrameEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame frame, ByteBuf out) throws Exception {
        ByteBuf pkg = ctx.alloc().buffer();
        frame.pack(pkg);
        //log.debug("send len:{}", pkg.readableBytes());
        out.writeBytes(pkg);
        pkg.release();
    }

}
