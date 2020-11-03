package com.alanpoi.im.lcs.util;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public final static String CRLF = "\r\n";

    public static boolean isNumber(String str) {
        if (str == null) return false;
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    public static boolean isAlphabet(String str) {
        if (str == null) return false;
        Pattern pattern = Pattern.compile("[a-zA-Z]*");
        return pattern.matcher(str).matches();
    }

    /*
     * 目前中国大陆地区的手机号
     * 移动：134、135、136、137、138、139、150、151、157(TD)、158、159、187、188
　　	   联通：130、131、132、152、155、156、185、186
　　	   电信：133、153、180、189、（1349卫通）
	   只做不太严格的判断，提升效率
     */
    public static boolean isChinessMobile(String str) {
        String regExp = "^[1][3|4|5|8][0-9]{9}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(str);
        return m.find();
    }

    public static String IdToUpperCase(String s) {
        if (s == null)
            s = "";
        s = s.trim().toUpperCase();
        s = s.replaceAll(" ", "_");
        s = s.replaceAll("_+", "_");
        return s;
    }

    public static void append(StringBuilder buf, byte b, int base) {
        int bi = 0xff & b;
        int c = '0' + (bi / base) % base;
        if (c > '9')
            c = 'a' + (c - '0' - 10);
        buf.append((char) c);
        c = '0' + bi % base;
        if (c > '9')
            c = 'a' + (c - '0' - 10);
        buf.append((char) c);
    }

    public static String getRandomString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String getRandomString2(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String[] split(String s, String regex, int limit) {
        if (s == null) return new String[0];
        return s.split(regex, limit);
    }

}
