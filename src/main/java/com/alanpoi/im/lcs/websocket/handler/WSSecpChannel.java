package com.alanpoi.im.lcs.websocket.handler;

import com.alanpoi.im.lcs.secprotocol.channel.AbstractSecpChannel;
import io.netty.channel.Channel;

/**
 * @author brandon
 * @create 2019-06-12
 *
 * 把websocket channel封装成安全Channel
 */

public class WSSecpChannel extends AbstractSecpChannel {

    public WSSecpChannel(Channel chnl){
        super(chnl);
    }
}
