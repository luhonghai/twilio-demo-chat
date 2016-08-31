package com.mhealth.chat.demo;

/**
 * Created by luhonghai on 8/30/16.
 */

public class ActionEvent {
    private final Action action;

    public ActionEvent(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        GROUP_ADDED,
        CHANNELS_UPDATED
    }
}
