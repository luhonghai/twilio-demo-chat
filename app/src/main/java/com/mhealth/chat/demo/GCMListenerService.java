package com.mhealth.chat.demo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class GCMListenerService extends FirebaseMessagingService
{
    private static final Logger logger = Logger.getLogger(GCMListenerService.class);

    @Override
    public void onMessageReceived(RemoteMessage message)
    {
        logger.d("onMessageReceived for GCM");
        Map data = message.getData();

        HashMap<String, String> pushNotification = new HashMap<String, String>();
        for (Object key : data.keySet()) {
            pushNotification.put(key.toString(), data.get(key).toString());
        }
        MainApplication.get().getBasicClient().getIpMessagingClient().handleNotification(
            pushNotification);
        notify(pushNotification, message);
    }

    private void notify(HashMap<String, String> data, RemoteMessage remoteMessage)
    {
        boolean isInApp = MainApplication.get().isInApplication();
        logger.d("Is in app " + isInApp);
        Log.d("Notification", "notify data " + new Gson().toJson(data));
        Log.d("Notification", "notify body " + remoteMessage.getNotification().getBody());
        Log.d("Notification", "notify title " + remoteMessage.getNotification().getTitle());
        Class<?> targetActivity = LoginActivity.class;
        String currentChannelId =  MainApplication.get().getCurrentChannelSid();
        String channelId = "";
        if (data.containsKey("channel_sid")) {
            channelId = data.get("channel_sid");
            if (isInApp) {
                targetActivity = MessageActivity.class;
            }
        } else {
            if (isInApp) {
                targetActivity = ChannelActivity.class;
            }
        }
        Intent intent = new Intent(this,targetActivity);
        if (!channelId.isEmpty())
            intent.putExtra("C_SID", channelId);
        if (!channelId.isEmpty() && !currentChannelId.isEmpty()
                && channelId.equalsIgnoreCase(currentChannelId)) {
            logger.d("Skip notification, user are in current channel " + channelId);
            return;
        }
        intent.putExtra("TARGET_ACTIVITY_CLASS", targetActivity.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String message;
        String title = "ManaDr Chat Notification";
        if (data.containsKey("channel_sid")) {
            message = data.get("text_message");
            if (message == null || message.isEmpty()) {
                message = remoteMessage.getNotification().getBody();
            }

        } else {
            message =remoteMessage.getNotification().getBody();
            if (remoteMessage.getNotification().getTitle() != null)
                title = remoteMessage.getNotification().getTitle();
        }

        PendingIntent pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(getApplicationContext().getResources().getColor(R.color.colorPrimary));

        NotificationManager notificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}
