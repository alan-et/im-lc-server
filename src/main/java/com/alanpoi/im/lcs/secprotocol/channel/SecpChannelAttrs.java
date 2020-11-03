package com.alanpoi.im.lcs.secprotocol.channel;

import io.netty.util.AttributeKey;

/**
 * @author brandon
 * @create 2019-06-12
 *
 * 安全Channel属性定义
 */

public class SecpChannelAttrs {
    //OldSecpChannel
    public static final AttributeKey<SecpChannel> SECPCHANNEL = AttributeKey.valueOf("OldSecpChannel");
    //socket关闭原因
    public static final AttributeKey<String>	  CLOSREASON = AttributeKey.valueOf("CloseReason");
}
