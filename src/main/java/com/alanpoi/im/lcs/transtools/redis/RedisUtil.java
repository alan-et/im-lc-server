package com.alanpoi.im.lcs.transtools.redis;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.*;

public class RedisUtil {

    private static Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    private JedisPool jedisPool;

    public RedisUtil(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Long expire(String key, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.expire(key, seconds);
        }
    }

    public Long del(String... keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(keys);
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public <T> T get(String key, Class<T> c) {
        String data = get(key);
        return JSON.parseObject(data, c);
    }

    public String setex(String key, int seconds, Object value) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.setex(key, seconds, JSON.toJSONString(value));
        }
    }

    public String getSet(String key, String val) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.getSet(key, val);
        }
    }

    public List<String> mget(String... keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.mget(keys);
        }
    }

    public List<String> mget(Collection<String> keys) {
        return mget(keys.toArray(new String[0]));
    }

    public <T> List<T> mget(Collection<String> keys, Class<T> c) {
        List<String> mget = mget(keys.toArray(new String[0]));
        List<T> res = new ArrayList<>(mget.size());
        for (String data : mget) {
            res.add(JSON.parseObject(data, c));
        }
        return res;
    }

    public long incrBy(String key, long delta) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incrBy(key, delta);
        }
    }

    public Long hdel(String key, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hdel(key, field);
        }
    }

    public Map<String, String> hgetAll(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(key);
        }
    }
    public String hget(String key, String field){
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hget(key, field);
        }
    }

    public Set<String> smembers(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.smembers(key);
        }
    }
    public List<Set<String>> smembers(List<String> keyList) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipelined = jedis.pipelined();
            for (String s : keyList) {
                pipelined.smembers(s);
            }
            List<Object> list = pipelined.syncAndReturnAll();
            List<Set<String>> res = new ArrayList<>(list.size());
            for (Object o : list) {
                res.add((Set<String>) o);
            }
            return res;
        }
    }

    public Long sadd(String key, String... members) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.sadd(key, members);
        }
    }

    public Long srem(String key, String members) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.srem(key, members);
        }
    }


    public long zcount(String key, double min, double max) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zcount(key, min, max);
        }
    }

    public long zcount(String key, String min, String max) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zcount(key, min, max);
        }
    }

    public long zadd(String key, double score, Object obj) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zadd(key, score, JSON.toJSONString(obj));
        }
    }


    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrangeByScore(key, min, max, offset, count);
        }
    }

    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrangeByScore(key, min, max, offset, count);
        }
    }

    public <T> List<T> zrangeByScore(String key, double min, double max, int offset, int count, Class<T> clazz) {
        Set<String> set = zrangeByScore(key, min, max, offset, count);
        List<T> res = new ArrayList<>(set.size());
        for (String data : set) {
            res.add(JSON.parseObject(data, clazz));
        }
        return res;
    }

    public Set<String> zrevranage(String key, long start, long end) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrange(key, start, end);
        }
    }

    public Set<String> zrevrangeByScore(String key, double min, double max, int offset, int count) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrangeByScore(key, max, min, offset, count);
        }
    }

    public Set<String> zrevrangeByScore(String key, String min, String max, int offset, int count) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrangeByScore(key, max, min, offset, count);
        }
    }

    public <T> List<T> zrevrangeByScore(String key, double min, double max, int offset, int count, Class<T> clazz) {
        Set<String> set = zrevrangeByScore(key, min, max, offset, count);
        List<T> res = new ArrayList<>(set.size());
        for (String data : set) {
            res.add(JSON.parseObject(data, clazz));
        }
        return res;
    }

    public Double zscore(String key, String member) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zscore(key, member);
        }
    }


}
