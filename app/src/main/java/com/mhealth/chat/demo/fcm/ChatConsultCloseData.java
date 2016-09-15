package com.mhealth.chat.demo.fcm;

import com.twilio.ipmessaging.UserInfo;

/**
 * Created by leanh215 on 9/15/16.
 */
public class ChatConsultCloseData extends NotificationData {

    String channelUniqueName;

    public ChatConsultCloseData(UserInfo userInfo, String channelUniqueName) {
        super(userInfo);
    }

    public String getChannelUniqueName() {
        return channelUniqueName;
    }

    public void setChannelUniqueName(String channelUniqueName) {
        this.channelUniqueName = channelUniqueName;
    }
}
