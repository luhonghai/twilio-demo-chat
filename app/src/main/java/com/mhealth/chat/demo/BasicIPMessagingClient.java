package com.mhealth.chat.demo;

import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.mhealth.chat.demo.data.UserPreference;
import com.twilio.common.AccessManager;

import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.Constants.CallbackListener;
import com.twilio.ipmessaging.IPMessagingClientListener;
import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.UserInfo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class BasicIPMessagingClient extends CallbackListener<IPMessagingClient>
    implements IPMessagingClientListener, AccessManager.Listener
{
    private static final Logger logger = Logger.getLogger(BasicIPMessagingClient.class);

    private String accessToken;
    private String gcmToken;

    private long              nativeClientParam;
    private IPMessagingClient ipMessagingClient;

    private Channel[] channels;
    private Context       context;
    private AccessManager accessManager;
    private LoginListener loginListener;
    private Handler       loginListenerHandler;
    private String        urlString;

    public BasicIPMessagingClient(Context context)
    {
        super();
        this.context = context;
    }

    public interface LoginListener {
        public void onLoginStarted();

        public void onLoginFinished();

        public void onLoginError(String errorMessage);

        public void onLogoutFinished();
    }

    public String getAccessToken()
    {
        return accessToken;
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

    public void doLogin(final String accessToken, final LoginListener listener, String url)
    {
        urlString = url;
        loginListenerHandler = setupListenerHandler();
        loginListener = listener;
        IPMessagingClient.setLogLevel(android.util.Log.DEBUG);
        createClientWithAccessManager();
    }

    public BasicIPMessagingClient()
    {
        super();
    }

    public List<Channel> getChannelList()
    {
        return Arrays.asList(channels);
    }

    public long getNativeClientParam()
    {
        return nativeClientParam;
    }

    public void setNativeClientParam(long nativeClientParam)
    {
        this.nativeClientParam = nativeClientParam;
    }

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
    public void onSuccess(IPMessagingClient client)
    {
        logger.d("Received completely initialized IPMessagingClient");
        ipMessagingClient = client;
        ipMessagingClient.setListener(this);
        setupGcmToken();

        PendingIntent pendingIntent =
            PendingIntent.getActivity(context,
                                      0,
                                      new Intent(context, ChannelActivity.class),
                                      PendingIntent.FLAG_UPDATE_CURRENT);
        ipMessagingClient.setIncomingIntent(pendingIntent);

        loginListenerHandler.post(new Runnable() {
            @Override
            public void run()
            {
                if (loginListener != null) {
                    loginListener.onLoginFinished();
                }
            }
        });
    }

    @Override
    public void onError(ErrorInfo errorInfo)
    {
        MainApplication.get().logErrorInfo("Received onError event", errorInfo);
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
        accessManager = new AccessManager(context, accessToken, new AccessManager.Listener() {
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

                IPMessagingClient.create(context.getApplicationContext(),
                                         accessManager,
                                         props,
                                         BasicIPMessagingClient.this);
            }

            @Override
            public void onError(AccessManager accessManager, String err)
            {
                logger.d("token error: " + err);
            }
        });
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

    @Override
    public void onTokenExpired(AccessManager accessManager)
    {
        logger.d("Received AccessManager:onTokenExpired.");
    }

    @Override
    public void onError(AccessManager accessManager, String err)
    {
        logger.d("Received AccessManager:onError. " + err);
    }

    @Override
    public void onTokenUpdated(AccessManager accessManager)
    {
        logger.d("Received AccessManager:onTokenUpdated.");
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
            handler = null;
            throw new IllegalArgumentException("Channel Listener must have a Looper.");
        }
        return handler;
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
                        logger.d("Updated Token was successfull");
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        logger.e("Updated Token failed");
                    }
                });
            }
            accessManager.updateToken(accessToken);
            createClientWithAccessManager();
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
}
