package com.mhealth.chat.demo.twilio;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mhealth.chat.demo.HttpHelper;
import com.mhealth.chat.demo.Logger;
import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.data.UserPreference;
import com.mhealth.chat.demo.event.MessageClientEvent;
import com.mhealth.chat.demo.service.MessageIncomingService;
import com.twilio.common.AccessManager;
import com.twilio.conversations.AudioOutput;
import com.twilio.conversations.IncomingInvite;
import com.twilio.conversations.LogLevel;
import com.twilio.conversations.TwilioConversationsClient;
import com.twilio.conversations.TwilioConversationsException;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants.CallbackListener;
import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.IPMessagingClientListener;
import com.twilio.ipmessaging.UserInfo;

import org.greenrobot.eventbus.EventBus;

public class TwilioClient
{
    private static final Logger logger = Logger.getLogger(TwilioClient.class);

    private String accessToken;
    private String gcmToken;
    private IPMessagingClient ipMessagingClient;
    private TwilioConversationsClient conversationsClient;
    private Context       context;
    private AccessManager accessManager;
    private String        urlString;

    private boolean isReady;

    public TwilioClient(Context context)
    {
        super();
        this.context = context;
        if(!TwilioConversationsClient.isInitialized()) {
            TwilioConversationsClient.setLogLevel(LogLevel.ERROR);
            TwilioConversationsClient.initialize(context);
        }
    }

    private AccessManager.Listener accessManagerListener = new AccessManager.Listener() {
        @Override
        public void onTokenExpired(AccessManager accessManager)
        {
            logger.d("token expired.");
            new GetAccessTokenAsyncTask().execute(urlString);
        }

        @Override
        public void onTokenUpdated(AccessManager accessManager)
        {
            logger.d("token updated. Creating client with valid token.");
            IPMessagingClient.Properties props =
                    new IPMessagingClient.Properties.Builder()
                            .setSynchronizationStrategy(
                                    IPMessagingClient.SynchronizationStrategy.ALL)
                            .setInitialMessageCount(50)
                            .createProperties();

            ipMessagingClient = IPMessagingClient.create(context.getApplicationContext(),
                    accessManager,
                    props,
                    ipMessagingClientCallbackListener);

            conversationsClient =
                    TwilioConversationsClient.create(accessManager,
                                    conversationClientListener);
            conversationsClient.setAudioOutput(AudioOutput.SPEAKERPHONE);
            conversationsClient.listen();


        }

        @Override
        public void onError(AccessManager accessManager, String err)
        {
            logger.d("token error: " + err);
        }
    };

    private CallbackListener<IPMessagingClient> ipMessagingClientCallbackListener = new CallbackListener<IPMessagingClient>() {
        @Override
        public void onSuccess(IPMessagingClient client)
        {
            logger.d("Received completely initialized IPMessagingClient");
            ipMessagingClient = client;
            ipMessagingClient.setListener(ipMessagingClientListener);
            setupGcmToken();
            PendingIntent pendingIntent =
                    PendingIntent.getService(context,
                            0,
                            new Intent(context, MessageIncomingService.class),
                            0);
            ipMessagingClient.setIncomingIntent(pendingIntent);
            EventBus.getDefault().post(new MessageClientEvent(MessageClientEvent.Type.READY, ipMessagingClient));
            isReady = true;
        }

        @Override
        public void onError(ErrorInfo errorInfo) {
            MainApplication.get().showError(errorInfo);
            EventBus.getDefault().post(new MessageClientEvent(MessageClientEvent.Type.ERROR, ipMessagingClient));
            isReady = false;
        }
    };

    private TwilioConversationsClient.Listener conversationClientListener = new TwilioConversationsClient.Listener() {
        @Override
        public void onStartListeningForInvites(TwilioConversationsClient twilioConversationsClient) {

        }

        @Override
        public void onStopListeningForInvites(TwilioConversationsClient twilioConversationsClient) {

        }

        @Override
        public void onFailedToStartListening(TwilioConversationsClient twilioConversationsClient, TwilioConversationsException e) {

        }

        @Override
        public void onIncomingInvite(TwilioConversationsClient twilioConversationsClient, IncomingInvite incomingInvite) {
            EventBus.getDefault().post(incomingInvite);
        }

        @Override
        public void onIncomingInviteCancelled(TwilioConversationsClient twilioConversationsClient, IncomingInvite incomingInvite) {

        }
    };

    private IPMessagingClientListener ipMessagingClientListener = new IPMessagingClientListener() {
        @Override
        public void onChannelAdd(Channel channel)
        {
            if (channel != null) {
                logger.d("A Channel :" + channel.getFriendlyName() + " got added");
            } else {
                logger.d("Received onChannelAdd event.");
            }
        }

        @Override
        public void onChannelChange(Channel channel)
        {
            if (channel != null) {
                logger.d("Channel Name : " + channel.getFriendlyName() + " got Changed");
            } else {
                logger.d("received onChannelChange event.");
            }
        }

        @Override
        public void onChannelDelete(Channel channel)
        {
            if (channel != null) {
                logger.d("A Channel :" + channel.getFriendlyName() + " got deleted");
            } else {
                logger.d("received onChannelDelete event.");
            }
        }

        @Override
        public void onClientSynchronization(IPMessagingClient.SynchronizationStatus status)
        {
            logger.e("Received onClientSynchronization callback with status " + status.toString());
        }

        @Override
        public void onUserInfoChange(UserInfo userInfo)
        {
            logger.e("Received onUserInfoChange callback");
        }

        @Override
        public void onChannelSynchronizationChange(Channel channel)
        {
            logger.e("Received onChannelSynchronizationChange callback " + channel.getFriendlyName());
        }

        @Override
        public void onError(ErrorInfo errorInfo) {
            MainApplication.get().logErrorInfo("Received onError event", errorInfo);
        }

        @Override
        public void onToastNotification(String channelId, String messageId)
        {
            setupListenerHandler().post(new Runnable() {
                @Override
                public void run()
                {
                    Toast.makeText(context, "Received new push notification", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }

        @Override
        public void onToastSubscribed()
        {
            setupListenerHandler().post(new Runnable() {
                @Override
                public void run()
                {
                    Toast.makeText(context, "Subscribed to push notifications", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }

        @Override
        public void onToastFailed(ErrorInfo errorInfo)
        {
            setupListenerHandler().post(new Runnable() {
                @Override
                public void run()
                {
                    Toast
                            .makeText(
                                    context, "Failed to subscribe to push notifications", Toast.LENGTH_LONG)
                            .show();
                }
            });
        }
    };

    public TwilioConversationsClient getConversationsClient() {
        return conversationsClient;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public String getGCMToken()
    {
        return gcmToken;
    }

    public void setGCMToken(String gcmToken)
    {
        this.gcmToken = gcmToken;
    }

    public void doLogin(String url)
    {
        urlString = url;
        accessToken = new UserPreference(context).getAccessToken();
        IPMessagingClient.setLogLevel(android.util.Log.DEBUG);
        if (accessToken == null || accessToken.isEmpty()) {
            new GetAccessTokenAsyncTask().execute(url);
        } else {
            createClientWithAccessManager();
        }
    }

    public IPMessagingClient getIpMessagingClient()
    {
        return ipMessagingClient;
    }

    private void setupGcmToken()
    {
        ipMessagingClient.registerGCMToken(getGCMToken(), new StatusListener() {
            @Override
            public void onError(ErrorInfo errorInfo)
            {
                MainApplication.get().showError(errorInfo);
                MainApplication.get().logErrorInfo("GCM registration not successful", errorInfo);
            }

            @Override
            public void onSuccess()
            {
                logger.i("GCM registration successful");
            }
        });
    }

    private void createClientWithAccessManager()
    {
        accessManager = new AccessManager(context, accessToken, accessManagerListener);
    }

    private Handler setupListenerHandler()
    {
        Looper  looper;
        Handler handler;
        if ((looper = Looper.myLooper()) != null) {
            handler = new Handler(looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            handler = new Handler(looper);
        } else {
            throw new IllegalArgumentException("Channel Listener must have a Looper.");
        }
        return handler;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    private class GetAccessTokenAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            new UserPreference(context).setAccessToken(accessToken);
            if (ipMessagingClient != null) {
                ipMessagingClient.updateToken(accessToken, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        logger.d("Updated Token was successfully");
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        logger.e("Updated Token failed");
                    }
                });
            }
            if (accessManager != null) {
                accessManager.updateToken(accessToken);
                conversationsClient = TwilioConversationsClient.create(accessManager, conversationClientListener);
            } else {
                createClientWithAccessManager();
            }
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params)
        {
            try {
                accessToken = new Gson().fromJson(HttpHelper.httpGet(params[0]), TokenData.class).getToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return accessToken;
        }
    }

    public void destroy() {
        try {
            TwilioConversationsClient.destroy();
        } catch (Exception e) {}
    }
}
