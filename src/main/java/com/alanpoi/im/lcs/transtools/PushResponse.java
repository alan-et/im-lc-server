package com.alanpoi.im.lcs.transtools;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaobin
 * @create 2019-07-20
 * <p>
 * LCS push请求响应
 */
public class PushResponse {
    private final int count;
    private List<byte[]> list;


    public PushResponse(int count) {
        this.count = count;
        list = new ArrayList<>(count);
    }

    /**
     * @param data 服务器响应数据
     * @return 是否所有请求都已响应
     */
    synchronized boolean addData(byte[] data) {
        list.add(data);
        return list.size() == count;
    }

    List<byte[]> getList() {
        return new ArrayList<>(list);
    }
}
