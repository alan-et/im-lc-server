package com.alanpoi.im.lcs.transtools;

import com.qzd.im.common.constants.TimeConstants;
import com.qzd.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.redis.RedisKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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

            String key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId());
            jedis.setex(key, TimeConstants.DAY_30, "" + System.currentTimeMillis());

            if (personId.getCompanyId() != null) {
                key = RedisKey.genKey(RedisKey.USER_ACTIVE, personId.getUserId(), personId.getCompanyId());
                jedis.setex(key, TimeConstants.DAY_30, "" + System.currentTimeMillis());
            }
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
            if (personId.getCompanyId() == null) {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId(), personId.getCompanyId());
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
            if (personId.getCompanyId() == null) {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId(), personId.getCompanyId());
            }
            keys.add(key);
        }
        if (keys.isEmpty()) return Collections.emptySet();
        Set<PersonId> res = new HashSet<>();
        List<String> values = null;
        try (Jedis jedis = jedisPool.getResource()) {
            values = jedis.mget(keys.toArray(new String[0]));
        }
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


}
