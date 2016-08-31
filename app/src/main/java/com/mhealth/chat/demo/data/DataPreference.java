package com.mhealth.chat.demo.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

/**
 * Created by luhonghai on 8/31/16.
 */

public class DataPreference<T extends TwilioObject> {

    private final Context context;

    private final SharedPreferences sharedPreferences;

    private final Class<T> clazz;

    public DataPreference(Context context, Class<T> clazz) {
        this.context = context;
        this.clazz = clazz;
        sharedPreferences = context.getSharedPreferences(clazz.getSimpleName(), Context.MODE_PRIVATE);
    }


    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public Context getContext() {
        return context;
    }

    public synchronized T get(String sid) {
        String data = sharedPreferences.getString(sid, "");
        if (data.isEmpty()) {
            return null;
        } else {
            return new Gson().fromJson(data, clazz);
        }
    }

    public synchronized void put(T object) {
        sharedPreferences.edit().putString(object.getSid(), new Gson().toJson(object)).apply();
    }
}
