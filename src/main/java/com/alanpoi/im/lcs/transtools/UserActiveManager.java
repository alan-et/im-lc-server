package com.alanpoi.im.lcs.transtools;

import com.alanpoi.im.common.constants.TimeConstants;
import com.alanpoi.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.redis.RedisKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.*;

/**
 * 活跃用户(30天进行过用户注册)管理
 */
public class UserActiveManager {
    private static Logger logger = LoggerFactory.getLogger(UserActiveManager.class);
    private JedisPool jedisPool;

    public UserActiveManager(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public boolean active(PersonId personId) {
        if (personId == null) return false;
        if (personId.getUserId() == null) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = null;
            if (StringUtils.isBlank(personId.getCompanyId())) {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId(), personId.getCompanyId());
            }
            jedis.setex(key, TimeConstants.DAY_30, "" + System.currentTimeMillis());

        } catch (Exception e) {
            logger.error("active {} error ", personId, e);
        }
        return true;
    }

    public boolean isActive(PersonId personId) {
        if (personId == null) return false;
        if (personId.getUserId() == null) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = null;
            if (StringUtils.isBlank(personId.getCompanyId())) {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId(), personId.getCompanyId());
            }
            String val = jedis.get(key);
            return val != null && val.trim().length() > 0;
        }
    }

    public Set<PersonId> getActivePersons(List<PersonId> personIds) {
        if (personIds == null || personIds.isEmpty()) return Collections.emptySet();

        List<String> keys = new ArrayList<>(personIds.size());
        for (PersonId personId : personIds) {
            String key = null;
            if (StringUtils.isBlank(personId.getCompanyId())) {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId(), personId.getCompanyId());
            }
            keys.add(key);
        }
        if (keys.isEmpty()) return Collections.emptySet();
        Set<PersonId> res = new HashSet<>();
        List<String> values = null;
        values = pipelinedMget(keys);
        if (values != null && values.size() > 0) {
            for (int i = 0; i < personIds.size(); ++i) {
                PersonId personId = personIds.get(i);

                String val = values.get(i);
                if (val != null && val.length() > 0) {
                    res.add(personId);
                }
            }
        }
        return res;
    }

    public Set<PersonId> getOnlinePersons(List<PersonId> personIds) {
        if (personIds == null || personIds.isEmpty()) return Collections.emptySet();

        List<String> keys = new ArrayList<>(personIds.size());
        for (PersonId personId : personIds) {
            String key = null;
            if (StringUtils.isBlank(personId.getCompanyId())) {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId(), personId.getCompanyId());
            }
            keys.add(key);
        }
        if (keys.isEmpty()) return Collections.emptySet();
        Set<PersonId> res = new HashSet<>();
        List<String> values = null;
        values = pipelinedMget(keys);
        if (values != null && values.size() > 0) {
            for (int i = 0; i < personIds.size(); ++i) {
                PersonId personId = personIds.get(i);

                String val = values.get(i);
                if (val != null && val.length() > 0) {
                    res.add(personId);
                }
            }
        }
        return res;
    }


    /**
     * 按下标顺序批量取值。
     *
     * <p>不能用 {@code jedis.mget(keys)}:ElastiCache Serverless 是集群模式,
     * 这些 key 按 userId 散落在不同 slot,MGET 会直接报 CROSSSLOT。
     * pipeline 逐个 GET 绕开 slot 限制,同时仍然只有一个 RTT ——
     * 这是判断"哪些人在线"的批量查询,在推送热路径上,不能退化成 N 次往返。</p>
     *
     * @return 与 keys 一一对应的值,不存在的位置为 null
     */
    private List<String> pipelinedMget(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipelined = jedis.pipelined();
            for (String key : keys) {
                pipelined.get(key);
            }
            List<Object> list = pipelined.syncAndReturnAll();
            List<String> res = new ArrayList<>(list.size());
            for (Object o : list) {
                res.add(o == null ? null : (String) o);
            }
            return res;
        }
    }
}
