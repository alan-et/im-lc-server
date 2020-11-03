package com.alanpoi.im.lcs.imsignal;

import com.alanpoi.im.lcs.transtools.LcsRegistry;
import com.qzd.im.common.model.PersonId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//用户长连接通道管理
@Component
public class UserChannelManager {

    private Map<Long, UserChannel> channelMap = new ConcurrentHashMap<>();

    //userId <-> channelId
    private Map<String, Set<Long>> userMap = new ConcurrentHashMap<>();
    //userId:companyId <-> channelId
    private Map<String, Set<Long>> personMap = new ConcurrentHashMap<>();

    @Autowired
    private LcsRegistry lcsResgistry;


    @PreDestroy
    public void stop() {
        //todo use batch method
        /*for (String userId : userMap.keySet()) {
            receiver.delUser(userId);
        }*/
        for (String personIdStr : personMap.keySet()) {
            PersonId personId = PersonId.parse(personIdStr);
            lcsResgistry.unregisterUser(personId.getUserId(), null);
            lcsResgistry.unregisterUser(personId.getUserId(), personId.getCompanyId());
        }
    }

    /**
     * 返回map中存储的旧连接
     */
    public UserChannel add(UserChannel channel) {
        Long channelId = channel.getChannelId();
        channelMap.put(channelId, channel);

        String userId = channel.getId().getUserId();
        addToMap(userMap, userId, channelId);
        //receiver.addUser(userId);

        String companyId = channel.getId().getCompanyId();
        addToMap(personMap, new PersonId(companyId, userId).toString(), channelId);
        //receiver.addPerson(userId, companyId);
        lcsResgistry.registerUser(userId, companyId);

        return channel;
    }

    public UserChannel remove(long channelId) {
        UserChannel channel = channelMap.remove(channelId);
        if (null == channel) {
            return null;
        }
        String userId = channel.getId().getUserId();
        String companyId = channel.getId().getCompanyId();

        removeFromMap(userMap, userId, channelId);
        removeFromMap(personMap, new PersonId(companyId, userId).toString(), channelId);

        List<UserChannel> userChannelList = findByUserId(userId);
        if (userChannelList == null || userChannelList.size() == 0) {
            //receiver.delUser(userId);
            lcsResgistry.unregisterUser(userId, null);
        }
        List<UserChannel> personChannelList = findByPersonId(userId, companyId);
        if (personChannelList == null || personChannelList.size() == 0) {
            //receiver.delPerson(userId, companyId);
            lcsResgistry.unregisterUser(userId, companyId);
        }

        return channel;
    }

    public List<UserChannel> findByUserId(String userId) {
        if (null == userId) {
            return null;
        }
        List<UserChannel> res = findFromMap(userMap, userId);
        if (null == res) {
            return null;
        }
        return res;
    }

    public List<UserChannel> findByPersonId(String userId, String companyId) {
        if (null == userId || null == companyId) {
            return null;
        }

        List<UserChannel> res = findFromMap(personMap, new PersonId(companyId, userId).toString());
        if (null == res) {
            return null;
        }
        return res;
    }

    private void addToMap(Map<String, Set<Long>> container, String key, long channelId) {
        Set<Long> ids = container.get(key);
        if (null == ids) {
            ids = new HashSet<>();
            container.putIfAbsent(key, ids);
            ids = container.get(key);
        }
        synchronized (ids) {
            ids.add(channelId);
        }

    }

    private void removeFromMap(Map<String, Set<Long>> container, String key, long channelId) {
        Set<Long> ids = container.get(key);
        if (null == ids) {
            return;
        }

        synchronized (ids) {
            ids.remove(channelId);
            if (ids.size() == 0) {
                container.remove(key);
            }
        }
    }

    private List<UserChannel> findFromMap(Map<String, Set<Long>> container, String key) {
        Set<Long> ids = container.get(key);
        if (null == ids) {
            return null;
        }
        Long[] curIds = null;

        synchronized (ids) {
            if (ids.size() > 0) {
                curIds = ids.toArray(new Long[0]);
            }
        }

        if (null == curIds) {
            return null;
        }

        List<UserChannel> res = new LinkedList<>();
        for (Long l : curIds) {
            UserChannel val = channelMap.get(l);
            if (null == val) {
                continue;
            }
            res.add(val);
        }
        if (res.size() < 1) {
            return null;
        }
        return res;
    }

    public List<UserChannel> findByPersonId(String userId, String companyId, List<Integer> clientTypes) {
        if (clientTypes == null || clientTypes.size() == 0) return Collections.emptyList();

        Set<Integer> clientTypeSet = new HashSet<>(clientTypes);
        List<UserChannel> channelList = findByPersonId(userId, companyId);
        if(channelList == null) return Collections.emptyList();

        if (clientTypeSet.contains(SignalProto.ClientType.ALL_VALUE)) return channelList;
        List<UserChannel> result = new ArrayList<>();
        for (UserChannel userChannel : channelList) {
            if (clientTypeSet.contains(userChannel.getId().getClientType())) {
                result.add(userChannel);
            }
        }
        return result;
    }

    public List<UserChannel> findByUserId(String userId, List<Integer> clientTypes) {
        if (clientTypes == null || clientTypes.size() == 0) return Collections.emptyList();

        Set<Integer> clientTypeSet = new HashSet<>(clientTypes);
        List<UserChannel> channelList = findByUserId(userId);
        if(channelList == null) return Collections.emptyList();

        if (clientTypeSet.contains(SignalProto.ClientType.ALL_VALUE)) return channelList;
        List<UserChannel> result = new ArrayList<>();
        for (UserChannel userChannel : channelList) {
            if (clientTypeSet.contains(userChannel.getId().getClientType())) {
                result.add(userChannel);
            }
        }
        return result;
    }

}
