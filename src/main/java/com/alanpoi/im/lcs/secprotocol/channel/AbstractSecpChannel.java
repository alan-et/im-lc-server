package com.alanpoi.im.lcs.secprotocol.channel;

import com.alanpoi.im.lcs.secprotocol.Cmd;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author brandon
 * @create 2019-06-12
 *
 * 安全Channel抽象实现
 */

public abstract class AbstractSecpChannel implements SecpChannel{
    private static final Logger log = LoggerFactory.getLogger(AbstractSecpChannel.class);

    //用来生成唯一id的种子
    private static AtomicLong idSeed = new AtomicLong(0);

    private long id;
    private Channel channel;
    private long timeStamp;
    private SecpChannelStatus status;

    public AbstractSecpChannel(Channel chnl){
        id = idSeed.incrementAndGet();
        if(0 == id){
            id = idSeed.incrementAndGet();
        }
        channel = chnl;
        timeStamp = System.currentTimeMillis();
        status = SecpChannelStatus.STATUS_NEW;
    }

    //channel的唯一id
    public long getId() {
        return id;
    }
    //真实的netty Channel类型
    public Channel getChannel(){
        return channel;
    }

    //时间戳
    public long getTimeStamp(){
        return timeStamp;
    }
    public void setTimeStamp(long timeStamp){
        this.timeStamp = timeStamp;
    }

    //状态
    public void setStatus(SecpChannelStatus status){
        this.status = status;
    }
    public SecpChannelStatus getStatus() {
        return status;
    }

    //关闭channel, reaseon:关闭的原因描述
    public void close(String reason) {
        if(SecpChannelStatus.STATUS_CLOSED.getVal() == status.getVal()) return;
        status = SecpChannelStatus.STATUS_CLOSED;

        Channel chnl = this.channel;
        if(null == chnl) return;

        if(!StringUtils.isEmpty(reason)){
            chnl.attr(SecpChannelAttrs.CLOSREASON).set(reason);
        }
        chnl.close();
    }

    //写数据
    public void write(SecpMessage msg){
        if(channel == null) {
            log.error("can't find channel. chnlId:{}", id);
            return;
        }
        channel.writeAndFlush(msg);
    }

    /**返回响应
     * @param req  请求的消息
     * @param body 响应时返回的body
     */
    public void respond(SecpMessage req, Object body){
        req.setBody(null);

        SecpMessage res = req;
        res.setCmd(Cmd.resCmd(req.getCmd()));
        res.setSeqId(req.getSeqId());
        if(!fillBody(res, body)){
            return;
        }

        write(res);
    }

    //推送消息
    public void push(Object body) {
        SecpMessage msg = new SecpMessage();
        msg.setCmd(Cmd.TOC_SIGNAL_REQ);
        if(!fillBody(msg, body)){
            return;
        }
        write(msg);
    }

    protected boolean fillBody(SecpMessage msg, Object body){
        if(null == body || null == msg){
            return true;
        }
        if(body instanceof String){
            String str = (String)body;
            if(!StringUtils.isEmpty(str)){
                byte[] bdata = str.getBytes();
                msg.setBody(bdata);
            }else{
                msg.setBody(null);
            }
        }else if(body instanceof byte[]){
            msg.setBody((byte[])body);
        }else if(body instanceof ByteBuf){
            ByteBuf buf = (ByteBuf)body;
            byte[] tmp = new byte[buf.readableBytes()];
            buf.readBytes(tmp);
            msg.setBody(tmp);
        }else{
            log.error("unknown body data type:{}", body.getClass().getName());
            return false;
        }
        return true;
    }

}
