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
        Intent intent = new Intent(this, MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data.containsKey("channel_sid")) {
            intent.putExtra("C_SID", data.get("channel_sid"));
        }
        Log.d("Notification", "notify message " + new Gson().toJson(data));
        String message;
        String title = "Twilio Notification";
        if (data.containsKey("channel_sid")) {
            message = data.get("text_message");
        } else {
            message =remoteMessage.getNotification().getBody();
            title = remoteMessage.getNotification().getTitle();
        }

        PendingIntent pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.rgb(214, 10, 37));

        NotificationManager notificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}
