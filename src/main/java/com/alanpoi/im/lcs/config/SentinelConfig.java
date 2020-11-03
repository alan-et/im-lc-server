//package com.alanpoi.im.lcs.config;
//
//import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
//import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
//import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
//import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
//import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
//import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
//import com.alibaba.fastjson.JSON;
//import com.qzd.im.common.sentinel.DefaultBlockExceptionHandler;
//import com.qzd.im.common.sentinel.ZkPathUtil;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.DependsOn;
//
//import javax.annotation.PostConstruct;
//import java.util.List;
//
//@Configuration
//@DependsOn("org.springframework.cloud.alibaba.sentinel.custom.SentinelAutoConfiguration")
//public class SentinelConfig {
//
//    @Value("${project.name}")
//    private String projectName;
//    @Value("${qzdim.sentinel.zk.addr}")
//    private String zkAddr;
//
//    @Bean
//    public SentinelResourceAspect sentinelResourceAspect() {
//        SentinelResourceAspect sra = new SentinelResourceAspect();
//        sra.setDefaultBlockExceptionHandler(new DefaultBlockExceptionHandler());
//        return sra;
//    }
//
//    @PostConstruct
//    private void init() {
//        if(zkAddr == null) throw new NullPointerException("qzdim.sentinel.zk.addr is null");
//        int idx = zkAddr.indexOf("//");
//        if (idx > 0) {
//            zkAddr = zkAddr.substring(idx + 2).trim();
//        }
//        ZookeeperDataSource<List<FlowRule>> zkds1 = new ZookeeperDataSource<>(
//                zkAddr,
//                ZkPathUtil.getFlowRuleZkPath(projectName),
//                s -> JSON.parseArray(s, FlowRule.class)
//        );
//
//        FlowRuleManager.register2Property(zkds1.getProperty());
//
//        ZookeeperDataSource<List<ParamFlowRule>> zkds2 = new ZookeeperDataSource<>(
//                zkAddr,
//                ZkPathUtil.getParamFlowRuleZkPath(projectName),
//                s -> JSON.parseArray(s, ParamFlowRule.class)
//        );
//
//        ParamFlowRuleManager.register2Property(zkds2.getProperty());
//    }
//}
