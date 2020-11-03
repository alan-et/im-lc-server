package com.alanpoi.im.lcs;

public enum IMError {
	SUCCESS(0, "SUCCESS"),
	UNKNOWN(1, "unknown"),

	SESSION_EXPIRED(0X0101, "session expired");


	
	private int code;
	private String msg;
	
	IMError(int code, String msg){
		this.code = code;
		this.msg = msg;
	}
	
	public int getCode(){
		return code;
	}
	public String getMsg(){
		return msg;
	}
}
