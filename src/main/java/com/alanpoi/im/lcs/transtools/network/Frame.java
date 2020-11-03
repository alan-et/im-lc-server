package com.alanpoi.im.lcs.transtools.network;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author brandon
 * @created 2019-07-18
 *
 * 消息类型
 */

/***
 * 数据报结构
 * BEGIN int32 数据报开始标志
 * LENGTH   int32 整个数据报的长度
 * CMD       int8  消息command
 * SEQID     int32 消息的序列号
 * CODE      int8  错误码
 * BODY      byte[] 消息体
 */
public class Frame {
    private static final int BEGIN = 0xFAED82D5;
    private static final int HLEN = 14; //报头长度
    private static final int MAXLEN = 4 * 1024 * 1024; //数据报最大长度限制

    //command定义
    public static final short CMD_RES = 0x80;

    public static final short CMD_HEARTBEAT = 0x01;  //心跳
    public static final short CMD_HEARTBEAT_RES = CMD_HEARTBEAT | CMD_RES;

    public static final short CMD_DATA = 0x02;//数据
    public static final short CMD_DATA_RES = CMD_DATA | CMD_RES;//数据

    ////////////////////////

    private static AtomicInteger idSeed = new AtomicInteger(0);

    ///////////////////////


    private short cmd;
    private int   seqId;
    private int   code;
    private byte[] body;

    public short getCmd() {
        return cmd;
    }

    public void setCmd(short cmd) {
        this.cmd = cmd;
    }

    public int getSeqId() {
        return seqId;
    }

    public void setSeqId(int seqId) {
        this.seqId = seqId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int bodyLength(){
        return null == body ? 0 : body.length;
    }

    @Override
    public Frame clone(){
        Frame res = new Frame();
        res.setCmd(cmd);
        res.setSeqId(seqId);
        res.setCode(code);
        res.setBody(body);
        return res;
    }

    public void pack(ByteBuf outstream){
        outstream.writeInt(BEGIN);
        int pkgLen = HLEN;
        if(null != body){
            pkgLen += body.length;
        }
        outstream.writeInt(pkgLen);
        outstream.writeByte((byte)(cmd & 0xff));
        outstream.writeInt(seqId);
        outstream.writeByte((byte)(code & 0xff));
        if(null != body){
            outstream.writeBytes(body);
        }
    }

    /**
     * 把二进制流解包成frame
     * @param instream 输入流
     * @return 如果能够成功的得到一个消息，返回这个消息，否则返回null
     */
    public static Frame unpack(ByteBuf instream) throws UnpackException{
        //检查长度
        if(instream.readableBytes() < HLEN){
            //不完整
            return null;
        }

        int ridx = instream.readerIndex();

        //找到起始位置
        int begin = instream.getInt(ridx + 0);
        if(BEGIN != begin){
            throw new UnpackException("can't find package BEGIN");
        }

        int pkgLen = instream.getInt(ridx + 4);
        if(pkgLen < HLEN || pkgLen > MAXLEN){
            //长度错误
            throw new UnpackException(String.format("invalid length:%d", pkgLen));
        }
        if(instream.readableBytes() < pkgLen){
            //不完整
            return null;
        }

        //反序列化一个完整的消息
        instream.readerIndex(ridx + 8);
        short cmd = (short)(instream.readByte() & 0xff);
        int seqId = instream.readInt();
        int code = instream.readByte() & 0xff;
        int bodyLen = pkgLen - HLEN;
        byte[] body = null;
        if(bodyLen > 0){
            body = new byte[bodyLen];
            instream.readBytes(body);
        }

        Frame frame = new Frame();
        frame.setCmd(cmd);
        frame.setSeqId(seqId);
        frame.setCode(code);
        frame.setBody(body);

        return frame;
    }

    public static int newSeqId(){
        return idSeed.incrementAndGet();
    }


    public static class UnpackException extends Exception {

        public UnpackException(String msg){
            super(msg);
        }
    }

}
