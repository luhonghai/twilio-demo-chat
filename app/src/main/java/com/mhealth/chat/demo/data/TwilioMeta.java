package com.mhealth.chat.demo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by luhonghai on 8/31/16.
 */

public class TwilioMeta {

    @SerializedName("key")
    @Expose
    private String key;

    @SerializedName("page")
    @Expose
    private int page;

    @SerializedName("page_size")
    @Expose
    private String pageSize;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }
}
