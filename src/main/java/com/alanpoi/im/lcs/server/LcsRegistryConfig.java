package com.alanpoi.im.lcs.server;

import com.alanpoi.im.lcs.transtools.LcsRegistry;
import com.alanpoi.im.lcs.transtools.redis.RedisLcsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class LcsRegistryConfig {
    private static Logger logger = LoggerFactory.getLogger(LcsRegistryConfig.class);

    @Value("${spring.redis.host:}")
    private String host;
    @Value("${spring.redis.port:}")
    private int port;
    @Value("${spring.redis.password:}")
    private String password;
    @Value("${spring.redis.timeout:}")
    private int timeout = 1000;
    @Value("${spring.redis.maxActive:}")
    private int maxActive = 100;
    @Value("${spring.redis.maxWait:}")
    private int maxWait = 5000;
    @Value("${spring.redis.maxIdle:}")
    private int maxIdle = 20;
    @Value("${spring.redis.minIdle:}")
    private int minIdle = 2;


    @Bean("RedisLcsRegistryPool")
    public JedisPool receiverJedisPool() {

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(maxWait);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        logger.info("receiverJedisPool config host:[{}] port:[{}] password:[{}] timeout:[{}] maxActive:[{}] maxWait:[{}] maxIdle:[{}] minIdle:[{}]",
                host, port, password, timeout, maxActive, maxWait, maxIdle, minIdle);
        return new JedisPool(config, host, port, timeout, password);
    }

    @Bean
    public LcsRegistry getLcsRegistry(@Qualifier("RedisLcsRegistryPool") JedisPool jedisPool){
        return new RedisLcsRegistry(4, jedisPool);
    }
}
