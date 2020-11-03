package com.alanpoi.im.lcs.concurrent;

public abstract class TimeoutRunnable implements Runnable{
	private long  timeStamp;
	private long  timeout;

	private Object commandInfo;

	public TimeoutRunnable(){
		timeStamp = 0;
		timeout = 0;
	}

	public void setTimeStamp(long timeStamp){
		this.timeStamp = timeStamp;
	}
	public void setTimeout(long timeout){
		this.timeout = timeout;
	}
	
	public boolean isTimeout(long now){
		if(0 >= timeStamp || 0 >= timeout){
			return false;
		}

		if(now - timeStamp > timeout){
			return true;
		}
		return false;
	}

	public void setCommandInfo(Object commandInfo) {
		this.commandInfo = commandInfo;
	}
	public Object getCommandInfo(){
		return commandInfo;
	}
}
