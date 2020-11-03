package com.alanpoi.im.lcs.transtools;

import com.qzd.im.common.model.PersonId;
import io.netty.util.concurrent.Promise;

import java.util.List;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS push请求类型
 */
public class PushRequest {

    private PersonId personId;

    private byte[] data;

    private long timeStamp;

    private int errorReason;


    private Promise<List<byte[]>> promise;

    public PersonId getPersonId() {
        return personId;
    }

    public void setPersonId(PersonId personId) {
        this.personId = personId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(int errorReason) {
        this.errorReason = errorReason;
    }

    public Promise<List<byte[]>> getPromise() {
        return promise;
    }

    public void setPromise(Promise<List<byte[]>> promise) {
        this.promise = promise;
    }
}
