package com.alanpoi.im.lcs.transtools.redis;

import com.alibaba.fastjson.JSON;
import com.qzd.im.common.constants.TimeConstants;
import com.qzd.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.AbstractLcsRegistry;
import com.alanpoi.im.lcs.transtools.LcsInfo;
import com.alanpoi.im.lcs.transtools.UserActiveManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Random;

/**
 * @author bradon
 * @create 2019-07-18
 *
 * Lcs注册中心Redis实现
 */

public class RedisLcsRegistry extends AbstractLcsRegistry {
    private static final Logger log = LoggerFactory.getLogger(RedisLcsRegistry.class);

    public static final int USER_SERVER_EXPIRE =  TimeConstants.DAY_7;

    private static final long SERVER_INFO_EXPIRE_TIME = TimeConstants.MINS_10 * 1000; //millisecond

    private JedisPool jedisPool;
    private RedisHelper helper;
    private UserActiveManager userActiveManager;

    public RedisLcsRegistry(int threads, JedisPool jedisPool) {
        super(threads);
        this.jedisPool = jedisPool;
        userActiveManager = new UserActiveManager(jedisPool);
        helper = new RedisHelper(this.jedisPool);
    }

    @Override
    protected void doInit() {
        LcsInfo exists = findServerInfo(lcs.getIp());
        if (exists == null) {
            registerServerInfo(lcs);
        } else {
            lcs = exists;
            log.info("use exists lcsInfo:{}",JSON.toJSONString(lcs));
            doHeartbeat();
        }
    }

    @Override
    protected void doDestroy() {
        //不删除，重启后复用id
//        delServerInfo(lcs);
    }

    @Override
    protected void doHeartbeat() {
        lcs.setHbtime(System.currentTimeMillis());
        updateServerInfo(lcs);
    }

    @Override
    protected void doClearInvalidLcsInfo() {
        cleanInvalidServer();
    }

    @Override
    protected void doRegisterUser(String userId, String companyId) {
        String strServerId = lcs.getStringId();
        try {
            userActiveManager.active(new PersonId(companyId, userId));

            String key = RedisKey.genKey(RedisKey.USER_SERVER, userId);
            doWriteServerId(key, strServerId);

            key = RedisKey.genKey(RedisKey.USER_SERVER, userId, companyId);
            doWriteServerId(key, strServerId);

            log.info("registerUser. userId:{}, companyId:{} lcsId:{}({})", userId, companyId, lcs.getId(), strServerId);
        }catch (Exception e){
            log.error("", e);
        }

    }

    private void doWriteServerId(String key, String strServerId) throws Exception{
        try(Jedis conn = getJedis()){
            String value = conn.get(key);

            if(null == value) value = "";
            if(value.indexOf(strServerId) >= 0){
                return;
            }
            conn.append(key, strServerId+",");
            conn.expire(key, USER_SERVER_EXPIRE);
        }
    }
    private void doDelSeverId(String key, String strServerId) throws Exception{
        try(Jedis conn = getJedis()) {
            String value = conn.get(key);
            if(null == value) return;
            String newValue = value.replace(strServerId+",", "");
            if(value.equals(newValue)){
                return;
            }

            String pre = conn.getSet(key, newValue);
            if(null == pre) pre = "";
            if(pre.equals("")) {
                //之前的值为空
                return;
            }
            if(pre.equals(value)){
                //设置过程中redis中的值没变化
                return;
            }

            //设置过程中redis中的值发生了变化
            //修复丢失的数据
            String[] arr = pre.split(",");
            if(null != arr && arr.length > 0){
                String appendStr = "";
                for(String svrId : arr){
                    if(newValue.indexOf(svrId) >= 0){
                        continue;
                    }
                    appendStr = appendStr + svrId + ",";
                }
                if(appendStr.length() > 0){
                    conn.append(key, appendStr);
                }
            }

        }
    }

    @Override
    protected void doUnregisterUser(String userId, String companyId) {
        if (userId == null){
            log.warn("doUnregisterUser userId is null");
            return;
        }

        String strServerId = lcs.getStringId();
        try {
            if (companyId == null) {
                String key = RedisKey.genKey(RedisKey.USER_SERVER, userId);
                doDelSeverId(key, strServerId);
            } else {
                String key = RedisKey.genKey(RedisKey.USER_SERVER, userId, companyId);
                doDelSeverId(key, strServerId);
            }
            log.info("unregisterUser. userId:{}, companyId:{} lcsId:{}({})", userId, companyId, lcs.getId(), strServerId);

        } catch (Exception e) {
            log.error("doUnregisterUser [{}]-[{}] error", userId, companyId, e);
        }
    }

    private Jedis getJedis() throws Exception{
        Jedis conn = jedisPool.getResource();
        if(null == conn){
            throw new Exception("can't get redis connection from pool");
        }

        return conn;
    }

    private LcsInfo findServerInfo(String ip) {
        for (LcsInfo lcsInfo : helper.getAllServerMapFromRedis().values()) {
            if (lcsInfo != null
                    && StringUtils.equals(ip, lcsInfo.getIp())) {
                return lcsInfo;
            }
        }
        return null;
    }

    //注册服务器信息
    private void registerServerInfo(LcsInfo serverInfo) {
        Jedis conn = null;
        try {
            conn = getJedis();
            Random random = new Random();
            String key = RedisKey.genKey(RedisKey.H_SERVER);
            int retryTimes = 1000;
            for (int i = 0; i < retryTimes; i++) {
                int id = (random.nextInt() + 1) & 0xFFFF; //16位以内 1-65535
                serverInfo.setId(id);


                Long res = conn.hsetnx(key, serverInfo.getStringId(), JSON.toJSONString(serverInfo));
                if (res != null && res == 1) {
                    log.info("register lcs info. id:{}, address:{}:{}", serverInfo.getId(), serverInfo.getIp(), serverInfo.getPort());
                    break;
                }

            }
        }catch (Exception e){
            log.error("", e);
        }finally {
            if(null != conn){
                conn.close();
            }
        }
    }

    private void updateServerInfo(LcsInfo serverInfo) {
        Jedis conn = null;
        try {
            conn = getJedis();

            String key = RedisKey.genKey(RedisKey.H_SERVER);
            conn.hset(key, serverInfo.getStringId(), JSON.toJSONString(serverInfo));
        }catch (Exception e){
            log.error("", e);
        }finally {
            if(null != conn){
                conn.close();
            }
        }
    }

    private boolean delServerInfo(String field) {
        try (Jedis conn = getJedis()){
            String key = RedisKey.genKey(RedisKey.H_SERVER);
            Long res = conn.hdel(key, field);
            if(null != res && 1 == res){
                return true;
            }else{
                return false;
            }
        }catch (Exception e){
            log.error("", e);
            return false;
        }
    }

    //清理无效的lcs信息
    private boolean cleanInvalidServer() {
        try {
            Map<String, LcsInfo> serverMap = helper.getAllServerMapFromRedis();

            boolean changed = false;
            for (Map.Entry<String, LcsInfo> entry : serverMap.entrySet()) {
                String field = entry.getKey();
                LcsInfo serverInfo = entry.getValue();
                boolean isHbTimeout = serverInfo != null
                        && System.currentTimeMillis() - serverInfo.getHbtime() > SERVER_INFO_EXPIRE_TIME;

                if (isHbTimeout) {
                    boolean success = delServerInfo(field);
                    if (success) {
                        log.info("remove invalid server : {}", JSON.toJSONString(serverInfo));
                        changed = true;
                    }
                }
            }
            return changed;
        }catch (Exception e){
            log.error("cleanInvalidServer error", e);
        }
        return false;
    }
}
