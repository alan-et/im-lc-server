package com.alanpoi.im.lcs;

import org.springframework.stereotype.Component;

@Component
public class CurrentTime {
	
	public static long getCurrentTime(){
		return System.currentTimeMillis();
	}
	
}
