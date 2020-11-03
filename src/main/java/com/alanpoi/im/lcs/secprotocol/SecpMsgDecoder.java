package com.alanpoi.im.lcs.secprotocol;

import com.alanpoi.im.lcs.CurrentTime;
import com.alanpoi.im.lcs.util.EncryptAES;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class SecpMsgDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(SecpMsgDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //String hex = ByteBufUtil.toHexString(in, in.readerIndex(), in.readableBytes());
        //log.info("recv data: {}", hex);
        try {
            //找到起始位置
            while(in.readableBytes() >= SecpMessage.C2S_HEADER_LENGTH){
                int begin = in.readInt();
                if(SecpMessage.PKG_BEGIN == begin){
                    in.readerIndex(in.readerIndex() - 4);
                    break;
                }
                in.readerIndex(in.readerIndex() - 3);
            }

            //检查长度
            if(in.readableBytes() < SecpMessage.C2S_HEADER_LENGTH){
                log.error("readableBytes {} < {}", in.readableBytes(), SecpMessage.C2S_HEADER_LENGTH);
                return;
            }

            int idx = in.readerIndex();
            int pkgLen = in.getShort(idx + 4) & 0xffff;
            if(pkgLen > SecpMessage.MAX_PKG_LENGTH){
                log.error("discarding data. pkgLen: {}", pkgLen);
                in.skipBytes(pkgLen);
                return;
            }
            if(pkgLen > in.readableBytes()){
                //不完整
                return;
            }

            //反序列化
            SecpMessage msg = bytesToMessage(in);
            //SecpMessage msg = JSON.parse(in.toString(CharsetUtil.UTF_8),SecpMessage.class);
            out.add(msg);

        }catch (Throwable e){
            in.skipBytes(in.readableBytes());
            throw e;
        }
    }

    private SecpMessage bytesToMessage(ByteBuf in){
        try {
            SecpMessage msg = new SecpMessage();
            msg.setMsgId(SecpMessage.newMsgId());
            msg.setStartTime(CurrentTime.getCurrentTime());
            msg.addTrace(String.format("start:%d", msg.getStartTime()));

            in.readInt();
            int pkgLen = in.readShort() & 0xffff;

            msg.setVersion((short)(in.readByte() & 0xff));
            msg.setCmd((short)(in.readByte() & 0xff));
            msg.setFlags((short)(in.readByte() & 0xff));
            msg.setLcId(in.readLong());
            msg.setSeqId(in.readInt());
            int bodyLen = pkgLen - SecpMessage.C2S_HEADER_LENGTH;
            msg.setBodyLength(bodyLen);
            if(bodyLen > 0){
                byte[] body = new byte[bodyLen];
                in.readBytes(body);
                msg.setBody(body);
            }else{
                msg.setBody(null);
            }

            return msg;

        }catch (Exception e){
            log.error("", e);
            return null;
        }

    }

    public static boolean uncompress(SecpMessage msg){
        if(!msg.isCompress()){
            return true;
        }
        byte[] body = msg.getBody();
        if(!(null != body && body.length > 0)){
            return true;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            GZIPInputStream ungzip = new GZIPInputStream(new ByteArrayInputStream(body));

            byte[] buf = new byte[4096];
            int rlen;
            while((rlen = ungzip.read(buf)) > 0){
                out.write(buf, 0, rlen);
            }

            byte[] data = out.toByteArray();
            msg.setBodyLength(data.length);
            msg.setBody(data);

        } catch (IOException e) {
            log.error("uncommpress gzip error. bodylen: %d", body.length);
            log.error("", e);
            return false;
        }
        //body = msg.getBody();
        //log.debug("uncompress len: {}", body.length);

        return true;
    }

    static public boolean decrypt(String key, SecpMessage msg){
        if(msg.getBodyLength() <= 0){
            return true;
        }

        byte[] body = msg.getBody();
        byte[] dorg = EncryptAES.decrypt128CBCPKCS5Padding(body, key);
        if(null == dorg){
            log.error("AES decrypt error. sessionId:{}");
            return false;
        }
        msg.setBodyLength(dorg.length);
        msg.setBody(dorg);

        return true;
    }




}
