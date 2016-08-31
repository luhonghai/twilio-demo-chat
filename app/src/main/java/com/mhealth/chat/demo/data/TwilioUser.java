package com.mhealth.chat.demo.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by luhonghai on 8/31/16.
 */

public class TwilioUser extends TwilioObject {

    public static class Attribute {
        @SerializedName("avatar_url")
        @Expose
        private String avatarUrl;

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }

    @SerializedName("identity")
    @Expose
    private String identity;

    @SerializedName("friendly_name")
    @Expose
    private String friendlyName;

    @SerializedName("attributes")
    @Expose
    private String attributes;

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public Attribute getAttributeObject() {
        if (attributes == null || attributes.isEmpty()) return null;
        return new Gson().fromJson(attributes, Attribute.class);
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getAttributes() {
        return attributes;
    }
}
