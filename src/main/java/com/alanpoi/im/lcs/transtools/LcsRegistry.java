package com.alanpoi.im.lcs.transtools;

import java.net.InetSocketAddress;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS服务注册中心接口
 */

public interface LcsRegistry {

    void init(InetSocketAddress local);
    void destroy();

    //注册信息
    void registerUser(String userId, String companyId);

    //注销用户信息
    void unregisterUser(String userId, String companyId);

}
