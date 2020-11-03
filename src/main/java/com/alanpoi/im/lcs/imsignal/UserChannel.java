package com.alanpoi.im.lcs.imsignal;

import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannel;
import com.google.protobuf.MessageLite;

import java.util.Objects;

//用户通道
public class UserChannel {
    private SecpChannel chnl;
    private ID id;
    private String token;
    private long timestamp;

    public UserChannel(SecpChannel chnl, ID key, String token) {
        this.chnl = chnl;
        this.id = key;
        this.token = token;
    }

    public ID getId() {
        return id;
    }
    public Long getChannelId(){
        return chnl.getId();
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return id.getUserId();
    }

    public int getClientType() {
        return id.getClientType();
    }

    public String getCompanyId() {
        return id.getCompanyId();
    }

    public void setTimestamp(long t) {
        this.timestamp = t;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void response(SecpMessage req, MessageLite resData) throws Exception {
        chnl.respond(req, resData.toByteArray());
    }

    public void push(SignalProto.SignalPush pdata) throws Exception {
        chnl.push(pdata.toByteArray());
    }

    public void close(String reason) {
        chnl.close(reason);
    }


    public static class ID {
        private String userId; //用户ID
        private int clientType; //客户端类型
        private String companyId; //公司ID
        private volatile int hashCode;

        public ID(String userId, int clientType) {
            this.userId = userId;
            this.clientType = clientType;

            this.hashCode = 0;
        }

        public ID(String userId, int clientType, String companyId) {
            this.userId = userId;
            this.clientType = clientType;
            this.companyId = companyId;
            this.hashCode = 0;
        }

        public String getUserId() {
            return userId;
        }

        public int getClientType() {
            return clientType;
        }

        public String getCompanyId() {
            return companyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ID id = (ID) o;
            return clientType == id.clientType &&
                    Objects.equals(userId, id.userId) &&
                    Objects.equals(companyId, id.companyId);
        }

        @Override
        public int hashCode() {
            if (hashCode != 0) return hashCode;

            hashCode = String.format("%s-%s-%d", userId, companyId, clientType).hashCode();
            return hashCode;
        }

        @Override
        public String toString() {
            return  String.format("%s-%s-%d", userId, companyId, clientType);
        }
    }

}
