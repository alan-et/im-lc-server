package com.alanpoi.im.lcs.event.model;

import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;

public class ReportProcessEvent extends UserChannelEvent {
    private SignalProto.UserProcessSig req;

    public ReportProcessEvent(UserChannel channel, SecpMessage secpMessage, SignalProto.ClientRequestHeader reqHeader,
                              SignalProto.UserProcessSig req) {
        super(channel, secpMessage, reqHeader);
        this.req = req;
    }

    public SignalProto.UserProcessSig getReq() {
        return req;
    }

    public void setReq(SignalProto.UserProcessSig req) {
        this.req = req;
    }
}
