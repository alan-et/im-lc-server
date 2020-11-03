package com.alanpoi.im.lcs.redis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;

/**
 * Redis 配置类
 * @author zhuoxun.peng
 */
@Configuration
public class RedisConfig {

    private Logger logger = LoggerFactory.getLogger(RedisConfig.class);


    /**
     * 节点名称
     */
    @Value("${redis.nodes:}")
    public String nodes;
    /**
     * 主机
     */
    @Value("${spring.redis.host:127.0.0.1}")
    public String host;

    /**
     * 端口
     */
    @Value("${spring.redis.port:6379}")
    public int port;

    /**
     * Redis服务名称
     */
    @Value("${redis.masterName:}")
    public String masterName;

    /**
     * 密码
     */
    @Value("${spring.redis.password:}")
    public String password;

    /**
     * 最大连接数
     */
    @Value("${spring.redis.maxActive:200}")
    public int maxTotal;

    @Value("${spring.redis.maxWait:6000}")
    public long maxWaitMillis;
    /**
     * 最大空闲数
     */
    @Value("${spring.redis.maxIdle:50}")
    public int maxIdle;

    /**
     * 最小空闲数
     */
    @Value("${spring.redis.minIdle:5}")
    public int minIdle;

    /**
     * 连接超时时间
     */
    @Value("${spring.redis.timeout:5000}")
    public int timeout;


    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(this.getMaxTotal());
        jedisPoolConfig.setMaxIdle(this.getMaxIdle());
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setMaxWaitMillis(this.getMaxWaitMillis());
        logger.info("JedisPool initialize end ...");
        return jedisPoolConfig;
    }

    @Bean
    public RedisTemplate<String, Serializable> redisTemplate() {
        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<String, Serializable>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        //设置序列化的模式
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        logger.info("RedisTemplate initialize end ...");
        return redisTemplate;
    }

    /**
     * jedis连接工厂
     *
     * @return
     */
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        JedisConnectionFactory jrcFactory = new JedisConnectionFactory();
        jrcFactory.setHostName(this.getHost());
        String password=this.getPassword();
        if (StringUtils.isNotEmpty(password)) {
            jrcFactory.setPassword(password);
        }
        jrcFactory.setPort(this.getPort());
        jrcFactory.setPoolConfig(jedisPoolConfig());
        return jrcFactory;
    }

}

