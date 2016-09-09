package com.mhealth.chat.demo.service;

import android.app.IntentService;
import android.content.Intent;

import com.mhealth.chat.demo.Logger;
import com.mhealth.chat.demo.event.ChannelEvent;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by luhonghai on 9/3/16.
 */

public class MessageIncomingService extends IntentService {

    private static final Logger logger = Logger.getLogger(MessageIncomingService.class);

    public MessageIncomingService() {
        super(MessageIncomingService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Channel channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
        String  action = intent.getStringExtra(Constants.EXTRA_ACTION);
        logger.d("On handle intent message incoming action " + action + " channel SID " + channel.getSid());
        if (action != null) {
            if (action.compareTo(Constants.EXTRA_ACTION_INVITE) == 0) {
                EventBus.getDefault().post(new ChannelEvent(ChannelEvent.Type.INVITE, channel));
            }
        }
    }
}
