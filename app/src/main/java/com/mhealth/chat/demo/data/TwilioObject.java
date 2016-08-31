package com.mhealth.chat.demo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by luhonghai on 8/31/16.
 */

public class TwilioObject {

    @SerializedName("sid")
    @Expose
    private String sid;

    @SerializedName("account_sid")
    @Expose
    private String accountSid;

    @SerializedName("service_sid")
    @Expose
    private String serviceSid;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getServiceSid() {
        return serviceSid;
    }

    public void setServiceSid(String serviceSid) {
        this.serviceSid = serviceSid;
    }
}
