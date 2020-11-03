package com.alanpoi.im.lcs.imsignal;

import com.alanpoi.im.lcs.IMError;

public class SignalException extends Exception {
    private int code;

    public SignalException(int code, String message){
        super(message);
        this.code = code;
    }
    public SignalException(IMError err){
        super(err.getMsg());
        this.code = err.getCode();
    }

    public int getCode(){
        return this.code;
    }
}
