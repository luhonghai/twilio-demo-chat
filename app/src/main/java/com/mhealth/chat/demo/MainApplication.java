package com.mhealth.chat.demo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.mhealth.chat.demo.data.DataPreference;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.data.TwilioUser;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.twilio.ipmessaging.ErrorInfo;

public class MainApplication extends Application
{
    private static MainApplication instance;

    private BasicIPMessagingClient   basicClient;

    private DataPreference<TwilioChannel> channelDataPreference;

    private DataPreference<TwilioUser> userDataPreference;

    public static MainApplication get()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        MainApplication.instance = this;
        basicClient = new BasicIPMessagingClient(getApplicationContext());
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
        Fresco.initialize(this);
    }

    public BasicIPMessagingClient getBasicClient()
    {
        return this.basicClient;
    }

    public void showError(final ErrorInfo error)
    {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run()
            {
                Toast
                    .makeText(getApplicationContext(),
                              String.format("Something went wrong. Error code: %s, text: %s",
                                            error.getErrorCode(),
                                            error.getErrorText()),
                              Toast.LENGTH_LONG)
                    .show();
            }
        });
    }

    public void logErrorInfo(final String message, final ErrorInfo error)
    {
        Log.e("MainApplication",
              String.format("%s. Error code: %s, text: %s",
                            message,
                            error.getErrorCode(),
                            error.getErrorText()));
    }

    public DataPreference<TwilioChannel> getChannelDataPreference() {
        if (channelDataPreference == null) {
            synchronized (this) {
                channelDataPreference = new DataPreference<>(this, TwilioChannel.class);
            }
        }
        return channelDataPreference;
    }

    public DataPreference<TwilioUser> getUserDataPreference() {
        if (userDataPreference == null) {
            synchronized (this) {
                userDataPreference = new DataPreference<>(this, TwilioUser.class);
            }
        }
        return userDataPreference;
    }
}
