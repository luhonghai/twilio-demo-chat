package com.mhealth.chat.demo.fcm;

import com.twilio.ipmessaging.UserInfo;

/**
 * Created by luhonghai on 9/13/16.
 */

public class ChatConsultRequestData extends NotificationData {

    private String sessionId;

    public ChatConsultRequestData(UserInfo myUserInfo) {
        super(myUserInfo);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
