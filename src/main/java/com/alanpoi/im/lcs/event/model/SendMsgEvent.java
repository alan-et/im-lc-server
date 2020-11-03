package com.alanpoi.im.lcs.event.model;

import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;

public class SendMsgEvent extends UserChannelEvent {
    private SignalProto.SendMsgReq req;

    public SendMsgEvent(UserChannel channel,SecpMessage secpMessage, SignalProto.ClientRequestHeader reqHeader, SignalProto.SendMsgReq req) {
        super(channel, secpMessage, reqHeader);
        this.req = req;
    }

    public SignalProto.SendMsgReq getReq() {
        return req;
    }

    public void setReq(SignalProto.SendMsgReq req) {
        this.req = req;
    }
}
