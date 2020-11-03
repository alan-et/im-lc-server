package com.alanpoi.im.lcs.transtools;

/***
 * @author brandon
 * @create 2019-07-19
 *
 * 推送异常类型
 */
public class PushException extends Exception {
    private int code;

    public PushException(int code, String msg){
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
