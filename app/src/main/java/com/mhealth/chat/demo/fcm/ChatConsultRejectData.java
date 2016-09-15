package com.mhealth.chat.demo.fcm;

import com.mhealth.chat.demo.fcm.ChatConsultRequestData;
import com.twilio.ipmessaging.UserInfo;

/**
 * Created by leanh215 on 9/14/16.
 */
public class ChatConsultRejectData extends ChatConsultRequestData {

    public ChatConsultRejectData(UserInfo myUserInfo) {
        super(myUserInfo);
    }
}
