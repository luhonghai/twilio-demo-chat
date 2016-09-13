package com.mhealth.chat.demo.fcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.twilio.ipmessaging.UserInfo;

/**
 * Created by luhonghai on 9/13/16.
 */

public abstract class NotificationData {
    /**
     * Sender FCM token
     */
    private String senderToken;
    /**
     * Sender identity
     */
    private String senderId;

    /**
     * Sender avatar URL
     */
    private String senderAvatarUrl;
    /**
     * Sender friendly name (Full name)
     */
    private String senderFriendlyName;

    public NotificationData() {

    }

    public NotificationData(UserInfo userInfo) {
        this.senderToken = FirebaseInstanceId.getInstance().getToken();
        this.senderId = userInfo.getIdentity();
        this.senderFriendlyName = userInfo.getFriendlyName();
        try {
            this.senderAvatarUrl = userInfo.getAttributes().optString("avatar_url");
        } catch (Exception e) {}
    }

    public String getSenderToken() {
        return senderToken;
    }

    public void setSenderToken(String senderToken) {
        this.senderToken = senderToken;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public String getSenderFriendlyName() {
        return senderFriendlyName;
    }

    public void setSenderFriendlyName(String senderFriendlyName) {
        this.senderFriendlyName = senderFriendlyName;
    }
}
