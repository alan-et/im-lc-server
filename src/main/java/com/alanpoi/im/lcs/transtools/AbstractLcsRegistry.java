package com.alanpoi.im.lcs.transtools;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS服务注册中心抽象实现
 */
public abstract class AbstractLcsRegistry implements LcsRegistry {

    private ScheduledThreadPoolExecutor executor;
    protected LcsInfo lcs;

    private volatile boolean isStart;


    public AbstractLcsRegistry(int threads){
        executor = new ScheduledThreadPoolExecutor(threads, new DefaultThreadFactory("LcsResistry"));
        isStart = false;
    }

    @Override
    public void init(InetSocketAddress local) {
        if(isStart) return;
        isStart = true;

        lcs = new LcsInfo();
        lcs.setIp(local.getAddress().getHostAddress());
        lcs.setPort(local.getPort());
        lcs.setHbtime(System.currentTimeMillis());
        lcs.setStarttime(System.currentTimeMillis());

        doInit();

        //启动定时心跳任务
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                doHeartbeat();
            }
        },1, 30, TimeUnit.SECONDS);

        //启动定时清理任务
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                doClearInvalidLcsInfo();
            }
        }, 1, 600, TimeUnit.SECONDS);

    }

    @Override
    public void destroy() {
        if(isStart) return;
        isStart = false;

        executor.shutdown();
        doDestroy();
    }

    @Override
    public void registerUser(String userId, String companyId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                doRegisterUser(userId, companyId);
            }
        });
    }


    @Override
    public void unregisterUser(String userId, String companyId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                doUnregisterUser(userId, companyId);
            }
        });
    }


    protected abstract void doInit();
    protected abstract void doDestroy();
    protected abstract void doHeartbeat();
    protected abstract void doClearInvalidLcsInfo();
    protected abstract void doRegisterUser(String userId, String companyId);
    protected abstract void doUnregisterUser(String userId, String companyId);

}
