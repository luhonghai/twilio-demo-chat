package com.mhealth.chat.demo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mhealth.chat.demo.data.UserPreference;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.IPMessagingClientListener;
import com.twilio.ipmessaging.UserInfo;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

@SuppressLint("InflateParams")
public class ChannelActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener
{


    private static final Logger logger = Logger.getLogger(ChannelActivity.class);

    private static final Handler handler = new Handler();
    private AlertDialog          incomingChannelInvite;
    private StatusListener       declineInvitationListener;

    @Bind(R.id.tabs)
    TabLayout tabLayout;

    @Bind(R.id.viewpager)
    ViewPager viewPager;

    Adapter adapter;

    @Bind(R.id.nav_view)
    NavigationView navigationView;

    BasicIPMessagingClient chatClient;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawer;

    private GoogleApiClient mGoogleApiClient;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        setupViewPager();
        setupTabLayout();
        navigationView.setNavigationItemSelectedListener(this);
        chatClient = MainApplication.get().getBasicClient();
        setNavigationHeader();
        setListener();
    }

    private void setListener()
    {
        chatClient.getIpMessagingClient().setListener(new IPMessagingClientListener() {
            @Override
            public void onChannelAdd(Channel channel)
            {
            }

            @Override
            public void onChannelChange(Channel channel)
            {

            }

            @Override
            public void onChannelDelete(Channel channel)
            {

            }

            @Override
            public void onError(ErrorInfo error)
            {
            }

            @Override
            public void onChannelSynchronizationChange(Channel channel)
            {
            }

            @Override
            public void onUserInfoChange(UserInfo userInfo)
            {
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                        setNavigationHeader();
                    }
                });
            }

            @Override
            public void onClientSynchronization(
                    IPMessagingClient.SynchronizationStatus synchronizationStatus)
            {
                if (synchronizationStatus == IPMessagingClient.SynchronizationStatus.CHANNELS_COMPLETED) {
                    EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.CHANNELS_UPDATED));
                }
            }

            @Override
            public void onToastNotification(String channelId, String messageId)
            {
            }

            @Override
            public void onToastSubscribed()
            {
            }

            @Override
            public void onToastFailed(ErrorInfo errorInfo)
            {
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mLogoutDialog != null && mLogoutDialog.isShowing()) {
            mLogoutDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setNavigationHeader() {

        View headerView = navigationView.getHeaderView(0);
        MenuItem logout = navigationView.getMenu().findItem(R.id.nav_logout);
        View userProfileView = headerView.findViewById(R.id.view_user_profile);
        if (userProfileView != null)
            userProfileView.setVisibility(View.VISIBLE);
        if (logout != null)
            logout.setVisible(true);
        if (chatClient != null && chatClient.getIpMessagingClient() != null) {
            UserInfo userInfo = chatClient.getIpMessagingClient().getMyUserInfo();
            final SimpleDraweeView imgUserProfile = (SimpleDraweeView) headerView.findViewById(R.id.img_user_profile);
            AppCompatTextView txtUserName = (AppCompatTextView) headerView.findViewById(R.id.txt_user_name);
            try {
                imgUserProfile.setImageURI(userInfo.getAttributes().optString("avatar_url"));
            } catch (Exception e) {e.printStackTrace();}
            txtUserName.setText(userInfo.getFriendlyName());
        }
    }



    private void setupViewPager() {
        adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(ChannelFragment.getInstance(Channel.ChannelType.PUBLIC), "Public", R.drawable.ic_public_black_24dp);
        adapter.addFragment(ChannelFragment.getInstance(Channel.ChannelType.PRIVATE), "Private", R.drawable.ic_security_black_24dp);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupTabLayout() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null)
                tab.setCustomView(adapter.getTabView(i));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View view = tab.getCustomView();
                if (view != null) {
                    TextView txtTitle = (TextView) view.findViewById(R.id.tab_title);
                    txtTitle.setTypeface(null, Typeface.BOLD);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View view = tab.getCustomView();
                if (view != null) {
                    TextView txtTitle = (TextView) view.findViewById(R.id.tab_title);
                    txtTitle.setTypeface(null, Typeface.NORMAL);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent intent = new Intent(this, AddGroupActivity.class);
                startActivityForResult(intent, ActivityResultCommon.ADD_GROUP);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        handleIncomingIntent(getIntent());
    }

    private boolean handleIncomingIntent(Intent intent)
    {
        if (intent != null) {
            Channel channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
            String  action = intent.getStringExtra(Constants.EXTRA_ACTION);
            intent.removeExtra(Constants.EXTRA_CHANNEL);
            intent.removeExtra(Constants.EXTRA_ACTION);
            if (action != null) {
                if (action.compareTo(Constants.EXTRA_ACTION_INVITE) == 0) {
                    this.showIncomingInvite(channel);
                }
            }
        }
        return false;
    }

    private void showIncomingInvite(final Channel channel)
    {
        handler.post(new Runnable() {
            @Override
            public void run()
            {
                if (incomingChannelInvite == null) {
                    incomingChannelInvite =
                            new AlertDialog.Builder(ChannelActivity.this)
                                    .setTitle(R.string.incoming_call)
                                    .setMessage(R.string.incoming_call_message)
                                    .setPositiveButton(
                                            R.string.join,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    channel.join(new StatusListener() {
                                                        @Override
                                                        public void onError(ErrorInfo errorInfo)
                                                        {
                                                            MainApplication.get().logErrorInfo(
                                                                    "Failed to join channel", errorInfo);
                                                        }

                                                        @Override
                                                        public void onSuccess()
                                                        {
                                                            //TODO notify update list
                                                            logger.d("Successfully joined channel");
                                                        }
                                                    });
                                                    incomingChannelInvite = null;
                                                }
                                            })
                                    .setNegativeButton(
                                            R.string.decline,
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    declineInvitationListener = new StatusListener() {

                                                        @Override
                                                        public void onError(ErrorInfo errorInfo)
                                                        {
                                                            MainApplication.get().logErrorInfo(
                                                                    "Failed to decline channel invite", errorInfo);
                                                        }

                                                        @Override
                                                        public void onSuccess()
                                                        {
                                                            logger.d("Successfully declined channel invite");
                                                        }

                                                    };
                                                    channel.declineInvitation(declineInvitationListener);
                                                    incomingChannelInvite = null;
                                                }
                                            })
                                    .create();
                    incomingChannelInvite.show();
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_logout) {
            logout();
        } else if (item.getItemId() == R.id.chat_group) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }
        }
        return false;
    }

    private void doLogout() {
        new UserPreference(this).setAccessToken("");
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        this.finish();
    }

    MaterialDialog mLogoutDialog;
    private void logout() {
        mLogoutDialog = new MaterialDialog.Builder(this)
                .title("Do you ready want to logout?")
                .positiveText("Logout")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        doLogout();
                    }
                })
                .negativeText("Cancel")
                .build();
        mLogoutDialog.show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    class Adapter extends FragmentStatePagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();
        private final List<Integer> mFragmentIcons = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title, int icon) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
            mFragmentIcons.add(icon);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        public View getTabView(int position) {
            View v = LayoutInflater.from(ChannelActivity.this).inflate(R.layout.tab_layout_home_page, null);
            TextView tv = (TextView) v.findViewById(R.id.tab_title);
            tv.setText(mFragmentTitles.get(position));
            TintableImageView img = (TintableImageView) v.findViewById(R.id.tab_icon);
            img.setImageResource(mFragmentIcons.get(position));
            return v;
        }
    }
}
