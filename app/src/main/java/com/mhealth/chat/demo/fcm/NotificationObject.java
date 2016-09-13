package com.mhealth.chat.demo.fcm;

import android.content.Intent;
import android.os.Bundle;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Created by luhonghai on 9/13/16.
 */

public class NotificationObject<T extends NotificationData> {

    public static final String KEY_TYPE = "type";
    public static final String KEY_DATA = "data";

    public enum Type {
        DEFAULT("default"),
        CHAT_CONSULT_REQUEST("CHAT_CONSULT_REQUEST", ChatConsultNotificationData.class),
        CHAT_CONSULT_REJECT("CHAT_CONSULT_REJECT", ChatConsultNotificationData.class),
        CHAT_CONSULT_ACCEPT("CHAT_CONSULT_ACCEPT", ChatConsultNotificationData.class),
        CHAT_CONSULT_CLOSE("CHAT_CONSULT_CLOSE", ChatConsultNotificationData.class),
        ;
        public static Type getByName(String name) {
            for (Type type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return DEFAULT;
        }
        String name;
        Class<? extends NotificationData> dataClass;

        public String getName() {
            return name;
        }

        Type( String name) {
            this(name, null);
        }

        Type( String name, Class<? extends NotificationData> dataClass) {
            this.name = name;
            this.dataClass = dataClass;
        }
    }

    public NotificationObject(Type type, T data) {
        this.type = type;
        this.data = data;
    }

    public static NotificationObject<? extends NotificationData> from(String type, String data) {
        Type mType = Type.getByName(type);
        return from(mType, data);
    }

    public static NotificationObject<? extends NotificationData> from(Type type, String data) {
        return new NotificationObject<>(type, data.length() > 0 ? new Gson().fromJson(data, type.dataClass) : null);
    }

    public static NotificationObject<? extends NotificationData> from(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return null;
        return from(bundle);
    }

    public static NotificationObject<? extends NotificationData> from(Bundle bundle) {
        if (!bundle.containsKey(KEY_TYPE)) return null;
        return from(bundle.getString(KEY_TYPE), bundle.containsKey(KEY_DATA) ? bundle.getString(KEY_DATA) : "");
    }

    public static NotificationObject<? extends NotificationData> from(Map<String, String> data) {
        if (!data.containsKey(KEY_TYPE)) return null;
        return from(data.get(KEY_TYPE), data.containsKey(KEY_DATA) ? data.get(KEY_DATA) : "");
    }

    private final T data;

    private final Type type;

    public String getDataJson() {
        if (data == null) return "";
        return new Gson().toJson(this.data);
    }

    public Type getType() {
        return type;
    }

    public T getData() {
        return data;
    }
}
