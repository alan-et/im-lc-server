package com.alanpoi.im.lcs.event.model;

import com.qzd.im.common.event2.Postable;

public class BackProcessEvent implements Postable {
    private String userId;
    private String companyId;
    private int clientType;

    public BackProcessEvent() {
    }

    public BackProcessEvent(String userId, String companyId, int clientType) {
        this.userId = userId;
        this.companyId = companyId;
        this.clientType = clientType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public int getClientType() {
        return clientType;
    }

    public void setClientType(int clientType) {
        this.clientType = clientType;
    }
}
