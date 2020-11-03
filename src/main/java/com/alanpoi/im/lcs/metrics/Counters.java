package com.alanpoi.im.lcs.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 总量统计
 *
 */

@Component
public class Counters {

    //自定义长连接通道建立(connected), 关闭(closed)的连接数
    public static final String SECP_CUSTOM_CONNECTED = "lcs.secp.connected";
    public static final String SECP_CUSTOM_CLOSED = "lcs.secp.closed";

    //websocket长连接通道(connected), 关闭(closed)的连接数
    public static final String SECP_WEBSOCKET_CONNECTED = "lcs.websocket.connected";
    public static final String SECP_WEBSOCKET_CLOSED = "lcs.websocket.closed";

    //信令推送
    public static final String PUSH_SIGNAL_TOTAL= "lcs.push.signal.total";
    @Autowired
    private MeterRegistry meterRegistry;

    public void increment(String key){
        increment(key, 1.0);
    }

    public void increment(String name, double amount){
        Counter counter = meterRegistry.counter(name);
        counter.increment(amount);
    }
}
