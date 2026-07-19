package com.alanpoi.im.lcs.event.model;

import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;

/** 客户端上行通话信令(cmd=CALL_SIGNAL) */
public class CallSignalEvent extends UserChannelEvent {
    private SignalProto.CallSignal req;

    public CallSignalEvent(UserChannel channel, SecpMessage secpMessage,
                           SignalProto.ClientRequestHeader reqHeader, SignalProto.CallSignal req) {
        super(channel, secpMessage, reqHeader);
        this.req = req;
    }

    public SignalProto.CallSignal getReq() {
        return req;
    }

    public void setReq(SignalProto.CallSignal req) {
        this.req = req;
    }
}
