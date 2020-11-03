package com.alanpoi.im.lcs.event.model;

import com.qzd.im.common.event2.Postable;
import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;

public abstract class UserChannelEvent implements Postable {
    private UserChannel userChannel;
    private SecpMessage secpMessage;
    private SignalProto.ClientRequestHeader reqHeader;

    public UserChannelEvent() {
    }

    public UserChannelEvent(UserChannel userChannel, SecpMessage secpMessage, SignalProto.ClientRequestHeader reqHeader) {
        this.userChannel = userChannel;
        this.secpMessage = secpMessage;
        this.reqHeader = reqHeader;
    }

    public UserChannel getUserChannel() {
        return userChannel;
    }

    public void setUserChannel(UserChannel userChannel) {
        this.userChannel = userChannel;
    }

    public SecpMessage getSecpMessage() {
        return secpMessage;
    }

    public void setSecpMessage(SecpMessage secpMessage) {
        this.secpMessage = secpMessage;
    }

    public SignalProto.ClientRequestHeader getReqHeader() {
        return reqHeader;
    }

    public void setReqHeader(SignalProto.ClientRequestHeader reqHeader) {
        this.reqHeader = reqHeader;
    }
}
