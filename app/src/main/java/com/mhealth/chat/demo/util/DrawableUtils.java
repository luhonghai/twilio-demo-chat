package com.mhealth.chat.demo.util;

import android.net.Uri;

import com.facebook.common.util.UriUtil;

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
}
