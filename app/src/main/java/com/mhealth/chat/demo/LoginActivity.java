package com.mhealth.chat.demo;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import com.mhealth.chat.demo.data.UserPreference;
import com.mhealth.chat.demo.event.MessageClientEvent;
import com.mhealth.chat.demo.twilio.TwilioClient;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.UserInfo;

import android.net.Uri;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends FragmentActivity implements
        GoogleApiClient.OnConnectionFailedListener
{

    private static final int RC_SIGN_IN = 1703;
    private static final Logger logger = Logger.getLogger(LoginActivity.class);

    private ProgressDialog         progressDialog;
    private String                 accessToken = null;
    private TwilioClient chatClient;
    private String                 endpoint_id = "";

    private boolean           isReceiverRegistered;
    private BroadcastReceiver registrationBroadcastReceiver;
    private static final int  PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private GoogleApiClient mGoogleApiClient;

    private GoogleSignInAccount acct;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EventBus.getDefault().register(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        registrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // progressDialog.dismiss();
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken =
                        sharedPreferences.getBoolean(GcmPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    logger.i("GCM token remembered");
                } else {
                    logger.w("GCM token NOT remembered");
                }
            }
        };
        // Registering BroadcastReceiver
        registerReceiver();
        chatClient = MainApplication.get().getBasicClient();
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    private void loginTwilio(GoogleSignInAccount acct) {
        progressDialog = ProgressDialog.show(LoginActivity.this, "", "Verify login information. Please wait...", true);
        this.acct = acct;
        String idChosen = acct.getEmail();
        logger.d("User id " + idChosen);
        this.endpoint_id =
                Secure.getString(this.getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        String endpointIdFull =
                idChosen + "-" + endpoint_id + "-android-" + getApplication().getPackageName();
        String url = Uri.parse(getString(R.string.api_url) + "/twilio_token.php")
                .buildUpon()
                .appendQueryParameter("identity", idChosen)
                .appendQueryParameter("endpointId", endpointIdFull)
                .build()
                .toString();
        logger.d("url string : " + url);
        LoginActivity.this.chatClient.doLogin(url);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            new UserPreference(this).setAccessToken("");
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            logger.d("Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = ProgressDialog.show(LoginActivity.this, "", "Checking. Please wait...", true);
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    dismissProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        logger.d("handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            if (acct != null) {
                loginTwilio(acct);
            } else {
                Toast.makeText(getBaseContext(), "Could not get login information", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Please login to access application", Toast.LENGTH_LONG).show();
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        }
    }

    @Subscribe
    public void onMessageClientEvent(MessageClientEvent event)
    {
        dismissProgressDialog();
        if (event.getType() == MessageClientEvent.Type.READY
                && acct != null && chatClient.getIpMessagingClient() != null) {
            doFinishLogin();
        } else {
            Toast.makeText(getBaseContext(), "Could not login. Please try again!", Toast.LENGTH_LONG).show();
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        }
    }

    private void doFinishLogin() {
        if (acct != null && chatClient.getIpMessagingClient() != null) {
            final UserInfo userInfo = chatClient.getIpMessagingClient().getMyUserInfo();
            if (userInfo != null) {
                userInfo.setFriendlyName(acct.getDisplayName(), new Constants.StatusListener() {
                    @Override
                    public void onSuccess() {
                        logger.d("Update user " + acct.getDisplayName() + " display name successfully!");
                    }
                });
                final String userAvatar = acct.getPhotoUrl() != null ?  acct.getPhotoUrl().toString() : "";
                logger.d("Try to load user avatar " + userAvatar);
                if (userAvatar.length() > 0) {
                    JSONObject attributes = userInfo.getAttributes();
                    if (attributes == null) {
                        attributes = new JSONObject();
                    }
                    try {
                        if (attributes.has("avatar_url")) {
                            attributes.remove("avatar_url");
                        }
                        attributes.put("avatar_url", userAvatar);
                    } catch (JSONException ignored) {
                        // whatever?
                    }
                    userInfo.setAttributes(attributes, new Constants.StatusListener() {
                        @Override
                        public void onSuccess() {
                            logger.d("Update user " + acct.getPhotoUrl() + " avatar successfully!");
                        }
                    });
                }
            } else {
                logger.e("No user found");
            }
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        Intent intent = new Intent(this, ChannelActivity.class);
        startActivity(intent);

        this.finish();
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(registrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver()
    {
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    registrationBroadcastReceiver,
                    new IntentFilter(GcmPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int                   resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                logger.i("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
