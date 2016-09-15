package com.mhealth.chat.demo.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.twilio.TwilioService;
import com.twilio.ipmessaging.Channel;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by luhonghai on 8/31/16.
 */

public class TwilioChannel extends TwilioObject {

    public static final String ATTR_CLOSED_CHANNEL = "closed_channel";
    public static final String ATTR_GROUP_ICON = "group_icon";

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public static class Attribute {
        @SerializedName("group_icon")
        @Expose
        private String icon;

        public String getIcon() {
            if (icon == null) return "";
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }

    @SerializedName("friendly_name")
    @Expose
    private String friendlyName;

    @SerializedName("unique_name")
    @Expose
    private String uniqueName;

    @SerializedName("created_by")
    @Expose
    private String createdBy;

    @SerializedName("attributes")
    @Expose
    private String attributes;

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Attribute getAttributeObject() {
        if (attributes == null || attributes.isEmpty()) return new Attribute();
        return new Gson().fromJson(attributes, Attribute.class);
    }

    public static void sync(Channel channel) {
        if (channel == null) return;
        sync(channel.getSid());
    }

    public static void sync(String sid) {
        TwilioService.getInstance().getChannel(sid)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<TwilioChannel>() {
                    @Override
                    public void call(TwilioChannel twilioChannel) {
                        MainApplication.get().getChannelDataPreference().put(twilioChannel);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }
}
