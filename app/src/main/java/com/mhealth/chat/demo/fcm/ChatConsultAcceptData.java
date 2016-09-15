package com.mhealth.chat.demo.fcm;

import com.twilio.ipmessaging.UserInfo;

/**
 * Created by leanh215 on 9/14/16.
 */
public class ChatConsultAcceptData extends ChatConsultRequestData{

    public ChatConsultAcceptData(UserInfo myUserInfo) {
        super(myUserInfo);
    }

    String channelUniqueName;



    public String getChannelUniqueName() {
        return channelUniqueName;
    }

    public ChatConsultAcceptData setChannelUniqueName(String channelUniqueName) {
        this.channelUniqueName = channelUniqueName;
        return this;
    }


}
