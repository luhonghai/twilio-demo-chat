package com.mhealth.chat.demo;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class DemoInstanceIDListenerService extends FirebaseInstanceIdService
{
    private static final Logger logger = Logger.getLogger(DemoInstanceIDListenerService.class);

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh()
    {
        logger.d("onTokenRefresh");
        // Fetch updated Instance ID token and notify our app's server of any changes.
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
