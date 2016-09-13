package com.mhealth.chat.demo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.mhealth.chat.demo.data.ChatConsultSession;
import com.mhealth.chat.demo.fcm.NotificationObject;
import com.mhealth.chat.demo.fcm.Const;
import com.twilio.ipmessaging.Channel;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

public class GCMListenerService extends FirebaseMessagingService
{
    private static final Logger logger = Logger.getLogger(GCMListenerService.class);

    @Override
    public void onMessageReceived(RemoteMessage message)
    {
        logger.d("onMessageReceived for FCM");

        String notifyType = message.getData().get("type");
        if (notifyType != null) {
            if (notifyType.equals(Const.FCM_TYPE_CHAT_CONSULTANT_REQUEST)) {

            } else if (notifyType.equals(Const.FCM_TYPE_CHAT_CONSULTANT_RESPONSE)) {

            }
        }


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

    private void notify(HashMap<String, String> data, RemoteMessage remoteMessage) {
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
        if (!channelId.isEmpty()) {
            intent.putExtra("C_SID", channelId);
            try {
                Channel channel = MainApplication.get().getBasicClient().getIpMessagingClient().getChannels().getChannel(channelId);
                if (channel.getUniqueName().toLowerCase().startsWith(ChatConsultSession.CHAT_CONSULT_PREFIX)) {
                    logger.d("Skip notification of chat consult channel " + channel.getUniqueName());
                    return;
                }
            } catch (Exception e) {

            }
        }
        if (!channelId.isEmpty() && !currentChannelId.isEmpty()
                && channelId.equalsIgnoreCase(currentChannelId)) {
            logger.d("Skip notification, user are in current channel " + channelId);
            return;
        }
        intent.putExtra("TARGET_ACTIVITY_CLASS", targetActivity.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String message;
        String title = "ManaDr Chat Notification";
        NotificationObject notificationObject = NotificationObject.from(data);
        if (notificationObject != null) {
            logger.d("Found notification object. Post event");
            EventBus.getDefault().post(notificationObject);
        }
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

    /**
     * Create and show a simple notification containing the received FCM message
     */
    private void sendNotification(PendingIntent pendingIntent, String title, String message) {
        CharSequence contentText = "";
        if (message != null && message.length() > 0) {
            contentText = Html.fromHtml(message);
        }
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        android.support.v4.app.NotificationCompat.Builder notificationBuilder = new android.support.v7.app.NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

}
