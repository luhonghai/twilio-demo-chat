package com.mhealth.chat.demo.util;

import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

/**
 * Created by leanh215 on 9/5/16.
 */
public class ChatUtils {

    public static String getAvatarUrl(Member member) {
        if (member.getUserInfo().getAttributes().has("avatar_url")) {
            return member.getUserInfo().getAttributes().optString("avatar_url");
        } else {
            return "file:///android_asset/raw/medical-icons/ic_contact.png";
        }
    }

    public static Member getChatUser(UserInfo currentUser, Channel privateChannel) {
        String currentUserId = currentUser.getIdentity();
        Member[] members = privateChannel.getMembers().getMembers();
        for (int i = 0 ; i < members.length; i++) {
            if (!members[i].getUserInfo().getIdentity().equals(currentUserId)) {
                return members[i];
            }
        }
        return null;
    }

    public static String generateChannelUniqueName(String sessionId) {
        return "chat_consult_" + sessionId;
    }

}
