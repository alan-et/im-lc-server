package com.alanpoi.im.lcs.secprotocol;

//命令字定义
public class Cmd {
    private static final short CMD_RES = 0x80;

    //协商密钥
    public static final short CREATE_SECKEY_REQ = 0x01;

    //向服务器发送应用层信令
    public static final short TOS_SIGNAL_REQ = 0x02;

    //向客户端推送应用层信令
    public static final short TOC_SIGNAL_REQ = 0x00;

    //心跳, 这个信令请求和返回是一样的
    public static final short HEARTBEAT_REQ = 0x03;

    //向客户端推送异常信令，此时code一定不是0
    public static final short TOC_EXCEPTION_REQ = 0x04;

    public static short resCmd(short reqCmd){
        short res = (short)(reqCmd | CMD_RES);
        return res;
    }

}
