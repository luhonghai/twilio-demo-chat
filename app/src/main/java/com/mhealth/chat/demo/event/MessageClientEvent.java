package com.mhealth.chat.demo.event;

import com.twilio.ipmessaging.IPMessagingClient;

/**
 * Created by luhonghai on 9/3/16.
 */

public class MessageClientEvent {

    private final Type type;

    private final IPMessagingClient client;

    public MessageClientEvent(Type type, IPMessagingClient client) {
        this.type = type;
        this.client = client;
    }

    public Type getType() {
        return type;
    }

    public IPMessagingClient getClient() {
        return client;
    }

    public enum Type {
        READY("READY"),
        ERROR("ERROR"),
        NONE("")
        ;
        String name;
        Type(String name) {
            this.name = name;
        }

        public static MessageClientEvent.Type getByName(String name) {
            for (MessageClientEvent.Type type : values()) {
                if (type.name.equalsIgnoreCase(name)) return type;
            }
            return NONE;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
