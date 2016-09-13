package com.mhealth.chat.demo.fcm;

import com.twilio.ipmessaging.UserInfo;

/**
 * Created by luhonghai on 9/13/16.
 */

public class ChatConsultNotificationData extends NotificationData {

    private String sessionId;

    public ChatConsultNotificationData(UserInfo myUserInfo) {
        super(myUserInfo);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
