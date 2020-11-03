package com.alanpoi.im.lcs.util;

public class IPUtil {

    private static final String aliHealthIpCidr = "100.64.0.0/10";
    private static final int mask = IPUtil.cidrToMask(aliHealthIpCidr);
    private static final int cidrIp = IPUtil.cidrToIp(aliHealthIpCidr);

    public static boolean isAliHealthIp(byte[] address) {
        int ip = IPUtil.byteIpToIntIp(address);
        return (ip & mask) == (cidrIp & mask);
    }

    public static int cidrToMask(String cidr) {
        if (cidr == null || cidr.length() == 0) throw new IllegalArgumentException("cidr error");
        String[] segments = cidr.split("/");
        if (segments.length != 2) throw new IllegalArgumentException("cidr error");

        int len = Integer.parseInt(segments[1]);
        return 0xFFFFFFFF << (32 - len);
    }

    public static int cidrToIp(String cidr) {
        if (cidr == null || cidr.length() == 0) throw new IllegalArgumentException("cidr error");
        String[] segments = cidr.split("/");
        if (segments.length != 2) throw new IllegalArgumentException("cidr error");

        return strIpToIntIp(segments[0]);
    }

    public static int strIpToIntIp(String ip){
        String[] ipSeg = ip.split("\\.");
        if (ipSeg.length != 4) throw new IllegalArgumentException("ip error");
        return (Integer.parseInt(ipSeg[0]) << 24) |
                (Integer.parseInt(ipSeg[1]) << 16) |
                (Integer.parseInt(ipSeg[2]) << 8) |
                (Integer.parseInt(ipSeg[3])/* << 0 */);
    }


    public static int byteIpToIntIp(byte[] bytes) {
        if (bytes == null || bytes.length != 4) throw new IllegalArgumentException("addr error");

        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF)/* << 0 */);
    }

    public static byte[] intIpToByteIp(int ip) {
        byte[] result = new byte[4];

        result[0] = (byte) (ip >> 24);
        result[1] = (byte) (ip >> 16);
        result[2] = (byte) (ip >> 8);
        result[3] = (byte) (ip /*>> 0*/);

        return result;
    }

    public static void main(String[] args) {
        //false
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.61.154.101"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.62.154.101"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.63.154.101"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("192.168.0.100"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("121.35.180.130"))));
        System.out.println("---");
        //true
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.64.0.0"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.64.154.101"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.65.154.101"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.66.154.101"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.121.108.221"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.121.109.27"))));
        System.out.println(isAliHealthIp(intIpToByteIp(strIpToIntIp("100.120.154.101"))));
    }

}
