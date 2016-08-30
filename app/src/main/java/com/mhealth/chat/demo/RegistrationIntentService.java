package com.mhealth.chat.demo;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

public class RegistrationIntentService extends IntentService
{
    private static final Logger logger = Logger.getLogger(RegistrationIntentService.class);

    private static final String[] TOPICS = { "global" };

    public RegistrationIntentService()
    {
        super("RegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            String     token = FirebaseInstanceId.getInstance().getToken();
            logger.i("GCM Registration Token: " + token);

            sendRegistrationToIPMClient(token);

            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(GcmPreferences.SENT_TOKEN_TO_SERVER, true).apply();
        } catch (Exception e) {
            logger.e("Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration
            // data, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(GcmPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(GcmPreferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to Twilio servers.
     *
     * @param token The new token.
     */
    private void sendRegistrationToIPMClient(String token)
    {
        TwilioApplication.get().getBasicClient().setGCMToken(token);
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    private void subscribeTopics(String token) throws IOException
    {
//        FcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic);
        }
    }
}
