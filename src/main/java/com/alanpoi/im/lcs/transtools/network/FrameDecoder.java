package com.alanpoi.im.lcs.transtools.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(FrameDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //String hex = ByteBufUtil.toHexString(in, in.readerIndex(), in.readableBytes());
        //log.info("recv data: {}", hex);
        //log.debug("recv data. len:{}", in.readableBytes());
        try {
            int frameCount = 0;
            while(in.readableBytes() > 0){
                int pre = in.readableBytes();
                //log.debug("before unpack. {}", pre);
                Frame frame = Frame.unpack(in);
                //log.debug("after unpack. {}", in.readableBytes());
                if(null == frame || pre == in.readableBytes()){
                    break;
                }
                out.add(frame);
                frameCount ++;
            }
            //log.debug("decode frame count:{}",frameCount);

        }catch (Throwable e){
            in.skipBytes(in.readableBytes());
            throw e;
        }
    }

}
