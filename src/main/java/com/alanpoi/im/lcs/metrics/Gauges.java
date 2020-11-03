package com.alanpoi.im.lcs.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 报告某个指标当前的值
 *
 */
@Component
public class Gauges {

    //正在创建的长连接数量: 自定义长连接
    public static final String SECP_CUSTOM_BUILDING_COUNT = "lcs.secp.longlink.building.count";

    //长连接数量：自定义长连接，websocket
    public static final String SECP_CUSTOM_LLCOUNT = "lcs.secp.longlink.count";
    public static final String SECP_WEBSOCKET_LLCOUNT = "lcs.websocket.longlink.count";

    @Autowired
    private MeterRegistry meterRegistry;

    public <T extends Number> void report(String name, T val){
        meterRegistry.gauge(name, val);
    }
}
