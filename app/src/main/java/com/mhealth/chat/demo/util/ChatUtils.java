package com.mhealth.chat.demo.util;

import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import org.json.JSONException;

/**
 * Created by leanh215 on 9/5/16.
 */
public class ChatUtils {

    public static String getAvatarUrl(Member member) {
        try {
            if (member.getUserInfo().getAttributes().has("avatar_url")) {
                return member.getUserInfo().getAttributes().getString("avatar_url");
            } else {
                return "file:///android_asset/raw/medical-icons/ic_contact.png";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
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

}
