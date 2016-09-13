package com.mhealth.chat.demo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mhealth.chat.demo.data.DataPreference;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.data.TwilioUser;
import com.mhealth.chat.demo.direct.MyLog;
import com.mhealth.chat.demo.twilio.TwilioClient;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.twilio.conversations.IncomingInvite;
import com.twilio.ipmessaging.ErrorInfo;

import io.fabric.sdk.android.Fabric;

public class MainApplication extends Application
{
    private static MainApplication instance;

    private TwilioClient basicClient;

    private DataPreference<TwilioChannel> channelDataPreference;

    private DataPreference<TwilioUser> userDataPreference;

    private IncomingInvite incomingInvite;

    private String currentChannelSid;

    private boolean isInApplication = false;

    public static MainApplication get()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        MainApplication.instance = this;
        basicClient = new TwilioClient(getApplicationContext());
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
        Fresco.initialize(this);

        MyLog.log("deviceToken=" + FirebaseInstanceId.getInstance().getToken());

    }

    public TwilioClient getBasicClient()
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

    public IncomingInvite getIncomingInvite() {
        return incomingInvite;
    }

    public void setIncomingInvite(IncomingInvite incomingInvite) {
        this.incomingInvite = incomingInvite;
    }

    public String getCurrentChannelSid() {
        if (currentChannelSid == null) return "";
        return currentChannelSid;
    }

    public void setCurrentChannelSid(String currentChannelSid) {
        this.currentChannelSid = currentChannelSid;
    }

    public boolean isInApplication() {
        return isInApplication;
    }

    public void setInApplication(boolean inApplication) {
        Log.d("Main", "setInApplication: " + inApplication);
        isInApplication = inApplication;
    }

    @Override
    public void onTerminate() {
        if (basicClient != null) basicClient.destroy();
        super.onTerminate();
    }
}
