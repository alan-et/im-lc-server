package com.alanpoi.im.lcs.event.model;

import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;

public class CallEvent extends UserChannelEvent {
    private SignalProto.OthBusiSig req;

    public CallEvent(UserChannel channel, SecpMessage secpMessage, SignalProto.ClientRequestHeader reqHeader, SignalProto.OthBusiSig req) {
        super(channel, secpMessage, reqHeader);
        this.req = req;
    }

    public SignalProto.OthBusiSig getReq() {
        return req;
    }

    public void setReq(SignalProto.OthBusiSig req) {
        this.req = req;
    }
}
