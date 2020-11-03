package com.alanpoi.im.lcs.secprotocol.channel;

/**
 * @author brandon
 * @create 2019-06-12
 *
 * 安全Channel状态定义
 */

public enum SecpChannelStatus {
    STATUS_NEW(1, "new channel"),
    STATUS_USED(2, "used channel"),
    STATUS_CLOSED(3, "closed channel")
    ;

    private int val;
    private String msg;

    SecpChannelStatus(int val, String msg){
        this.val = val;
        this.msg = msg;
    }

    public int getVal() {
        return val;
    }

    public String getMsg() {
        return msg;
    }
}
