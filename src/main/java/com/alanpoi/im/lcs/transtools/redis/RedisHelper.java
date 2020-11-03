package com.alanpoi.im.lcs.transtools.redis;

import com.alibaba.fastjson.JSON;
import com.alanpoi.im.lcs.transtools.LcsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author bradon
 * @create 2019-07-18
 *
 * redis操作辅助类
 */
public class RedisHelper {
    private static final Logger logger = LoggerFactory.getLogger(RedisHelper.class);

    private final JedisPool jedisPool;
    private final RedisUtil redisUtil;

    public RedisHelper(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.redisUtil = new RedisUtil(jedisPool);
    }

    public RedisUtil getRedisUtil() {
        return redisUtil;
    }

    public Map<String, LcsInfo> getAllServerMapFromRedis() {
        String key = RedisKey.genKey(RedisKey.H_SERVER);
        Map<String, String> serverMap = redisUtil.hgetAll(key);

        Map<String, LcsInfo> tempAllServerMap = new HashMap<>();
        for (Map.Entry<String, String> entry : serverMap.entrySet()) {

            LcsInfo serverInfo = null;
            try {
                serverInfo = JSON.parseObject(entry.getValue(), LcsInfo.class);
            } catch (Exception e) {
                logger.warn("getAllServerMapFromRedis parse error, data:[{}] ", entry.getValue(), e);
            }
            if (serverInfo != null && serverInfo.getStringId() != null) {
                tempAllServerMap.put(entry.getKey(), serverInfo);
            }
        }
        return tempAllServerMap;
    }

    public LcsInfo getLcsInfo(String lcsId){
        String key = RedisKey.genKey(RedisKey.H_SERVER);
        String strRes = redisUtil.hget(key, lcsId);

        if(StringUtils.isEmpty(strRes)){
            return null;
        }

        LcsInfo serverInfo = null;
        try {
            serverInfo = JSON.parseObject(strRes, LcsInfo.class);
        } catch (Exception e) {
            logger.warn("getLcsInfo parse error, data:[{}] ", strRes, e);
        }

        return serverInfo;
    }

    public static <K, V> boolean compareMap(Map<K, V> map1, Map<K, V> map2) {
        if (map1 == map2) return true;
        if (map1 == null || map2 == null) return false;
        if (map1.size() != map2.size()) return false;
        for (Map.Entry<K, V> entry : map1.entrySet()) {
            K k1 = entry.getKey();
            V v1 = entry.getValue();
            V v2 = map2.get(k1);
            if (!Objects.equals(v1, v2)) return false;
        }
        return true;
    }
}
