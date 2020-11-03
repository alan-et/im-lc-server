package com.alanpoi.im.lcs.config;

import com.qzd.im.common.id.ID;
import com.qzd.im.common.id.ServerID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class IdConfig {
    public static final String SPACE = "lcs";

    @Bean(destroyMethod = "destroy")
    public ServerID serverID(@Autowired JedisPool jedisPool) {
        ServerID serverID = new ServerID(SPACE, jedisPool);
        ID.config(serverID.getId());
        return serverID;
    }
}
