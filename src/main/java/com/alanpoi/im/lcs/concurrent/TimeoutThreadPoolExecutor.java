package com.alanpoi.im.lcs.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TimeoutThreadPoolExecutor extends ThreadPoolExecutor {
    private static final Logger log = LoggerFactory.getLogger(TimeoutThreadPoolExecutor.class);

    private long 						timeout;

    private String poolName;

    private AtomicLong  requestCount = new AtomicLong(0); //总请求数
    private AtomicLong  consumedCount = new AtomicLong(0); //总消费数
    private AtomicLong  rejectedCount = new AtomicLong(0); //总抛弃数

    private AtomicInteger   runningThreads = new AtomicInteger(0); //运行中的线程数

    public TimeoutThreadPoolExecutor(String poolName, int poolSize, int commandQueueSize, long timeout) {
        super(poolSize, poolSize, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(commandQueueSize), new DefaultThreadFactory(poolName));

        this.timeout = timeout;

        this.poolName = poolName;
    }

    public void execute(TimeoutRunnable command){
        try {
            long now = System.currentTimeMillis();
            command.setTimeStamp(now);
            command.setTimeout(timeout);
            requestCount.incrementAndGet();
            super.execute(command);
        }catch (Exception e){
            log.error("threadpoolExecute error:", e);
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r){
        consumedCount.incrementAndGet();
        runningThreads.incrementAndGet();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t){
        runningThreads.decrementAndGet();
    }

    public String getPoolName(){
        return poolName;
    }

    public long getRequestCount(){
        return requestCount.get();
    }
    public long getConsumedCount(){
        return consumedCount.get();
    }
    public long getRejectedCount(){
        return rejectedCount.get();
    }
    public int getRunningThreads(){
        return runningThreads.get();
    }
    public int getQueueLength(){
        return this.getQueue().size();
    }


    private class RejectedHandler implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public RejectedHandler() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.warn("command rejected");
            rejectedCount.incrementAndGet();
        }
    }
}
