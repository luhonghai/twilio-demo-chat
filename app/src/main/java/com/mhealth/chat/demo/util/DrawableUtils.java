package com.mhealth.chat.demo.util;

import android.net.Uri;

import com.facebook.common.util.UriUtil;
import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.twilio.ipmessaging.Member;

import org.json.JSONObject;

/**
 * Created by luhonghai on 8/31/16.
 */

public class DrawableUtils {

    public static Uri getResourceURI(int id) {
        return new Uri.Builder()
                .scheme(UriUtil.LOCAL_RESOURCE_SCHEME) // "res"
                .path(String.valueOf(id))
                .build();
    }

    public static void fillUserReachability(SimpleDraweeView reachabilityView, Member member) {
        if (!MainApplication.get().getBasicClient().getIpMessagingClient().isReachabilityEnabled()) {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_block_black_24dp));
            reachabilityView.setColorFilter(reachabilityView.getContext().getResources().getColor(R.color.colorOrange));
        } else if (member.getUserInfo().isOnline()) {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_online_black_24dp));
            reachabilityView.setColorFilter(reachabilityView.getContext().getResources().getColor(R.color.colorPrimary));
        } else if (member.getUserInfo().isNotifiable()) {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_online_black_24dp));
            reachabilityView.setColorFilter(reachabilityView.getContext().getResources().getColor(R.color.colorGray));
        } else {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_lens_black_24dp));
            reachabilityView.setColorFilter(reachabilityView.getContext().getResources().getColor(R.color.colorGray));
        }
    }

    public static void fillUserAvatar(SimpleDraweeView avatarView, Member member)
    {
        try {
            avatarView.setTag(member);
            JSONObject attributes = member.getUserInfo().getAttributes();
            String avatar = (String) attributes.opt("avatar_url");
            avatarView.setImageURI(avatar);
        } catch (Exception e) {}
    }
}
