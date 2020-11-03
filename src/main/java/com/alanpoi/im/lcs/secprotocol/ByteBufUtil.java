package com.alanpoi.im.lcs.secprotocol;

import io.netty.buffer.ByteBuf;

public class ByteBufUtil {
    private static String HEX_STR = "0123456789ABCDEF";

    public static String toHexString(ByteBuf in, int start, int length){
        StringBuffer res = new StringBuffer();
        for(int i = start; i < start + length; ++ i){
            int b = in.getByte(i) & 0xff;
            char ch = HEX_STR.charAt((b & 0xf0) >> 4) ;
            char cl = HEX_STR.charAt(b & 0x0f);
            res.append(ch);
            res.append(cl);
        }

        return res.toString();
    }
}
