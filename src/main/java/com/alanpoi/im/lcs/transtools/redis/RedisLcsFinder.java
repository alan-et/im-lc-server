package com.alanpoi.im.lcs.transtools.redis;

import com.qzd.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.AbstractLcsFinder;
import com.alanpoi.im.lcs.transtools.LcsInfo;
import com.alanpoi.im.lcs.transtools.network.TcpClientConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS服务finder类型的redis实现
 */
public class RedisLcsFinder extends AbstractLcsFinder {
    private static final Logger log = LoggerFactory.getLogger(RedisLcsFinder.class);

    private static final int LCS_INVALIDMARK_EXPIRE = 10000;

    private RedisHelper helper;


    private Map<String, LcsInvalidMark> lcsInvalidMarkMap = new ConcurrentHashMap<>();

    public RedisLcsFinder(TcpClientConnector connector, JedisPool jedisPool){
        super(connector);
        helper = new RedisHelper(jedisPool);
    }


    @Override
    public LcsInfo getLcsInfo(String lcsId) {
        LcsInvalidMark mark = lcsInvalidMarkMap.get(lcsId);
        if (null != mark) {
            if (System.currentTimeMillis() - mark.getTimeStamp() >= LCS_INVALIDMARK_EXPIRE) {
                lcsInvalidMarkMap.remove(lcsId);
                mark = null;
            }
        }
        if (null != mark) {
            return null;
        }

        LcsInfo lcsInfo = helper.getLcsInfo(lcsId);
        if (null == lcsInfo) {
            log.warn("lcsInfo {} is invalid", lcsId);
            mark = new LcsInvalidMark();
            mark.setTimeStamp(System.currentTimeMillis());
            lcsInvalidMarkMap.put(lcsId, mark);
            return null;
        }

        return lcsInfo;
    }

    @Override
    public Map<PersonId, Set<String>> getOnlinePersons(List<PersonId> personIds) {
        if (personIds == null || personIds.isEmpty()) return Collections.emptyMap();
        List<String> keys = new ArrayList<>();
        for(int i = 0; i < personIds.size(); ++ i) {
            PersonId personId = personIds.get(i);
            String key = null;
            String userId = personId.getUserId();
            if(null == userId) userId = "";
            String companyId = personId.getCompanyId();
            if(null == companyId) companyId = "";

            if (companyId.equals("")) {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId());
            } else {
                key = RedisKey.genKey(RedisKey.USER_SERVER, personId.getUserId(), personId.getCompanyId());
            }
            keys.add(key);
        }
        if (keys.isEmpty()) return Collections.emptyMap();

        Map<PersonId, Set<String>> res = new HashMap<>();

        List<String> values = helper.getRedisUtil().mget(keys);
        for(int i = 0; i < personIds.size(); ++ i){
            PersonId personId = personIds.get(i);

            String val = values.get(i);
            if (val == null || val.trim().length() == 0) continue;

            String[] arr = val.split(",");
            if(null == arr || 0 == arr.length){
                continue;
            }

            Set<String> svrIds = res.get(personId);
            if(null == svrIds){
                svrIds = new HashSet<>();
                res.put(personId, svrIds);
            }
            svrIds.addAll(Arrays.asList(arr));
        }
        return res;
    }


    private class LcsInvalidMark {
        private long timeStamp;

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }
    }

}
