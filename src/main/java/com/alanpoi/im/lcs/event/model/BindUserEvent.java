package com.alanpoi.im.lcs.event.model;

import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;

public class BindUserEvent extends UserChannelEvent {
    private SignalProto.BindUserSig req;

    public BindUserEvent(UserChannel channel,SecpMessage secpMessage, SignalProto.ClientRequestHeader reqHeader,
                         SignalProto.BindUserSig req) {
        super(channel, secpMessage, reqHeader);
        this.req = req;
    }

    public SignalProto.BindUserSig getReq() {
        return req;
    }

    public void setReq(SignalProto.BindUserSig req) {
        this.req = req;
    }
}
