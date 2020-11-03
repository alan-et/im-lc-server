package com.alanpoi.im.lcs.transtools.redis;


public class RedisKey {
    public static final String SPACE = "transtools";
    public static final String SEPARATOR = ":";

    //hash server信息 {space}:h_server 持久
    public static final String H_SERVER = "h_server";

    //String 用户所在服务器 {space}:userServer:{userId} 非持久
    //string 用户在公司所在服务器 {space}:userServer:{userId}:{companyId} 非持久
    public static final String USER_SERVER = "userServer";

    //String 用户最后一次活跃时间 {space}:userActive:{userId} 非持久
    //string 用户在公司最后一次活跃时间 {space}:userActive:{userId}:{companyId} 非持久
    public static final String USER_ACTIVE = "userActive";


    public static String genKey(Object... keys) {

        return SPACE + SEPARATOR + join(keys, SEPARATOR);
    }


    public static String join(Object[] array, String separator) {
        return array == null ? null : join(array, separator, 0, array.length);
    }

    public static String join(Object[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        } else {
            if (separator == null) {
                separator = "";
            }

            int noOfItems = endIndex - startIndex;
            if (noOfItems <= 0) {
                return "";
            } else {
                StringBuilder buf = new StringBuilder(noOfItems * 16);

                for(int i = startIndex; i < endIndex; ++i) {
                    if (i > startIndex) {
                        buf.append(separator);
                    }

                    if (array[i] != null) {
                        buf.append(array[i]);
                    }
                }

                return buf.toString();
            }
        }
    }
}
