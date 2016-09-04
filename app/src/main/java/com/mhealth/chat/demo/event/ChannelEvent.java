package com.mhealth.chat.demo.event;

import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;

/**
 * Created by luhonghai on 9/3/16.
 */

public class ChannelEvent {

    private final Type type;

    private final Channel channel;

    public ChannelEvent(Type type, Channel channel) {
        this.type = type;
        this.channel = channel;
    }

    public Type getType() {
        return type;
    }

    public Channel getChannel() {
        return channel;
    }

    public enum Type {
        INVITE(Constants.EXTRA_ACTION_INVITE),
        NONE("")
        ;
        String name;
        Type(String name) {
            this.name = name;
        }

        public static Type getByName(String name) {
            for (Type type : values()) {
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
