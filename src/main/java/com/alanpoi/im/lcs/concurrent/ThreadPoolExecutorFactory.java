package com.alanpoi.im.lcs.concurrent;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorFactory {
	private ConcurrentHashMap<String, TimeoutThreadPoolExecutor> executors = new ConcurrentHashMap<String, TimeoutThreadPoolExecutor>();
	private Object lock = new Object();

	private static ThreadPoolExecutorFactory instance = null;
	private static Object instanceLock = new Object();

	public static ThreadPoolExecutorFactory getInstance(){
		if(null == instance){
			synchronized (instanceLock){
				if(null == instance){
					instance = new ThreadPoolExecutorFactory();
				}
			}
		}
		return instance;
	}

	private ThreadPoolExecutorFactory(){}

	
	public TimeoutThreadPoolExecutor createTimeoutExecutor(String name, int poolSize, int commandQueueSize, long timeout){
		TimeoutThreadPoolExecutor exe = executors.get(name);
		if(null == exe){
			synchronized(lock){
				exe = executors.get(name);
				if(null == exe){
					exe = new TimeoutThreadPoolExecutor(name, poolSize, commandQueueSize, timeout);
					executors.put(name, exe);
				}
			}
		}
		
		return exe;
	}
	
	public TimeoutThreadPoolExecutor getTimeoutExecutor(String name){
		return executors.get(name);
	}

	public Enumeration<String> getExecutorNames(){
		return executors.keys();
	}
	
	public void shutdownAll(){
		synchronized (lock) {
			for (ThreadPoolExecutor exe : executors.values()) {
				exe.shutdown();
			}
			executors.clear();
		}
	}

	
}
