package com.mhealth.chat.demo;

/**
 * Created by luhonghai on 8/27/16.
 */

public class CallEvent {

    private String target;

    public CallEvent(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
