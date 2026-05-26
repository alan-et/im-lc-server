package com.alanpoi.im.lcs.transtools;

import java.net.InetSocketAddress;

/**
 * @author pengzhuoxun
 * @create 2019-07-18
 *
 * LCS服务注册中心接口
 */

public interface LcsRegistry {

    void init(InetSocketAddress local);
    void destroy();

    //注册信息
    void registerUser(String userId);

    //注销用户信息
    void unregisterUser(String userId);

}
