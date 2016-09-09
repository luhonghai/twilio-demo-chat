package com.mhealth.chat.demo.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by luhonghai on 8/30/16.
 */

public class UserPreference {

    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    private final SharedPreferences sharedPreferences;

    public UserPreference(Context context) {
        sharedPreferences = context.getSharedPreferences(ACCESS_TOKEN, Context.MODE_PRIVATE);
    }

    public void setAccessToken(String token) {
        sharedPreferences.edit().putString(ACCESS_TOKEN, token).apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(ACCESS_TOKEN, "");
    }
}
