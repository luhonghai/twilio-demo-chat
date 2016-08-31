package com.mhealth.chat.demo.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by luhonghai on 8/31/16.
 */

public class TwilioChannel extends TwilioObject {

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
        if (attributes == null || attributes.isEmpty()) return null;
        return new Gson().fromJson(attributes, Attribute.class);
    }
}
