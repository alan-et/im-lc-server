package com.alanpoi.im.lcs.secprotocol.channel;

import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import io.netty.channel.Channel;

/**
 * @author brandon
 * @create 2019-06-12
 *
 * 安全协议通道类型
 */

public interface SecpChannel {

    //channel的唯一id
    long getId();

    //真实的netty Channel类型
    Channel getChannel();

    //时间戳
    long getTimeStamp();
    void setTimeStamp(long timeStamp);

    //状态
    void setStatus(SecpChannelStatus status);
    SecpChannelStatus getStatus();

    //关闭channel, reaseon:关闭的原因描述
    void close(String reason);

    //写数据
    void write(SecpMessage msg);

    /**返回响应
     * @param req  请求的消息
     * @param body 响应时返回的body
     */
    void respond(SecpMessage req, Object body);

    //推送消息
    void push(Object body);
}
