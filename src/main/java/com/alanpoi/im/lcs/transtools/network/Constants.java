package com.alanpoi.im.lcs.transtools.network;

/***
 * @author brandon
 * @created 2019-07-18
 */
public class Constants {
    public static final int HEARTBEAT_INTERVAL = 30; //心跳事件间隔
    public static final int READ_TIMEOUT = HEARTBEAT_INTERVAL + 5; //读超时时间
}
