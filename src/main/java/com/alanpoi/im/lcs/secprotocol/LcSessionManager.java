package com.alanpoi.im.lcs.secprotocol;

import com.qzd.im.common.id.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author brandon
 * @create 2019-06-12
 *
 * 长连接会话管理器
 */

@Component
public class LcSessionManager {
    private static final Logger log = LoggerFactory.getLogger(LcSessionManager.class);

    private static final int lcsTimeout = 3600 * 24; //会话超时时间
    private static final String lcsPre = "LCSession";

    @Autowired
    private StringRedisTemplate redis;


    public long newLcId(){
        return ID.next();
    }

    public String getSecKey(long lcId){
        try {
            String key = lcsPre + ":" + lcId;
            String secKey = redis.opsForValue().get(key);
            return secKey;
        }catch (Exception e){
            log.error("get LCSession error");
            return null;
        }
    }

    public void saveSecKey(long lcId, String secKey){
        try {
            String key = lcsPre + ":" + lcId;
            redis.opsForValue().set(key, secKey, lcsTimeout, TimeUnit.SECONDS);
        }catch (Exception e){
            log.error("save LCSession error: lcId:{}", lcId);
        }
    }

}
