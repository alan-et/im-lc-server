package com.alanpoi.im.lcs.secprotocol;

import com.alanpoi.im.lcs.util.EncryptAES;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class SecpMsgEncoder extends MessageToByteEncoder<SecpMessage> {
    private static final Logger log = LoggerFactory.getLogger(SecpMsgEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, SecpMessage msg, ByteBuf out) throws Exception {
        //log.info("encode msg");
        int bodyLength = 0;
        byte[] body = msg.getBody();
        if(null != body){
            bodyLength = body.length;
        }
        msg.setBodyLength(bodyLength);
        int pkgLen = bodyLength + SecpMessage.S2C_HEADER_LENGTH;

        ByteBuf pkg = ctx.alloc().buffer();
        pkg.writeInt(SecpMessage.PKG_BEGIN);
        pkg.writeShort((short)(pkgLen & 0xffff));
        pkg.writeByte(SecpMessage.VERRSION);
        pkg.writeByte((byte)(msg.getCmd() & 0xff));
        pkg.writeByte((byte)(msg.getFlags() & 0xff));
        pkg.writeLong(msg.getLcId());
        pkg.writeInt(msg.getSeqId());
        pkg.writeByte((byte)(msg.getCode() & 0xff));
        if(null != body && body.length > 0){
            pkg.writeBytes(body);
        }
        out.writeBytes(pkg);
        pkg.release();
    }


    static public void compress(SecpMessage msg){
        byte[] body = msg.getBody();
        msg.setCompress(false);
        if(!(null != body && body.length > 0)){
            msg.setBodyLength(0);
            return;
        }

        int orgBodyLength = body.length;
        msg.setBodyLength(orgBodyLength);
        if(orgBodyLength <= SecpMessage.COMPRESS_THRESHOLD){
            return;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(body);
            gzip.close();
        } catch ( Exception e) {
            log.error("", e);
            return;
        }
        msg.setCompress(true);
        byte[] data = out.toByteArray();
        msg.setBodyLength(data.length);
        msg.setBody(data);
        //log.info("compress from {} to {}", orgBodyLength, data.length);

    }

    static public boolean encrypt(String key, SecpMessage msg){
        byte[] body = msg.getBody();
        if(!(null != body && body.length > 0)){
            return true;
        }

        byte[] data = EncryptAES.encrypt128CBCPKCS5Padding(body, key);
        if(null == data){
            //log.error("encrypt error");
            return false;
        }
        //log.info("aes encrypt from {} to {}", dsrc.length, data.length);

        msg.setBodyLength(data.length);
        msg.setBody(data);

        return true;
    }
}
