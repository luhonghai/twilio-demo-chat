package com.mhealth.chat.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.adapter.IconAdapter;
import com.mhealth.chat.demo.adapter.IconViewListener;
import com.mhealth.chat.demo.adapter.SelectMemberAdapter;
import com.mhealth.chat.demo.adapter.SelectMemberViewListener;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.data.TwilioUser;
import com.mhealth.chat.demo.twilio.TwilioService;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Members;
import com.twilio.ipmessaging.UserInfo;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.ribot.easyadapter.EasyAdapter;

public class ChannelDetailActivity extends AppCompatActivity implements MemberViewHolder.TwilioChannelView,IconViewListener, SelectMemberViewListener {

    private static final int MAX_MEMBER_SIZE = 256;

    private Channel currentChannel;

    @Bind(R.id.channel_friendly_name)
    TextView txtName;

    @Bind(R.id.txt_leave_group)
    TextView txtLeave;

    @Bind(R.id.txt_remove_group)
    TextView txtRemove;

    @Bind(R.id.txt_member_count)
    TextView txtMemberCount;

    @Bind(R.id.txt_group_created_time)
    TextView txtCreatedStatus;

    @Bind(R.id.list_members)
    ListView listMembers;

    @Bind(R.id.img_group)
    ImageView imgGroup;

    List<Member> members;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    IPMessagingClient client;

    MaterialDialog dialogAction;

    private Subscription subscription;
    private Subscription subscriptionUser;

    TwilioChannel twilioChannel;

    List<TwilioUser> twilioUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(Channel.class.getName())) {
            currentChannel = (Channel) bundle.get(Channel.class.getName());
            client = MainApplication.get().getBasicClient().getIpMessagingClient();
            showGroupInfo();


            subscription = TwilioService.getInstance().getChannel(currentChannel.getSid())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<TwilioChannel>() {
                        @Override
                        public void call(TwilioChannel twilioChannel) {
                            ChannelDetailActivity.this.twilioChannel = twilioChannel;
                            final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.US);
                            txtCreatedStatus.setText("Group created "
                                    + " by " + getMemberName(twilioChannel.getCreatedBy())
                                    + " on "
                                    + sdf.format(currentChannel.getDateCreatedAsDate()));
                            txtCreatedStatus.setVisibility(View.VISIBLE);
                            if (client.getMyUserInfo().getIdentity().equalsIgnoreCase(twilioChannel.getCreatedBy())) {
                                txtRemove.setVisibility(View.VISIBLE);
                            } else {
                                txtRemove.setVisibility(View.GONE);
                            }
                            loadMembers();

                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                            loadMembers();
                        }
                    });

            subscriptionUser = TwilioService.getInstance().listUsers(1000,0)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<TwilioService.TwilioUsersResponse>() {
                        @Override
                        public void call(TwilioService.TwilioUsersResponse twilioUsersResponse) {
                            ChannelDetailActivity.this.twilioUsers = twilioUsersResponse.getUsers();
                            try {
                                Log.d("", "Receive members list size " + ChannelDetailActivity.this.twilioUsers.size());
                            } catch (Exception e) {}
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });

        } else {
            this.finish();
        }
    }

    @OnClick(R.id.add_member)
    public void clickAddMember() {
        if (isAdmin() || currentChannel.getType() == Channel.ChannelType.PUBLIC) {
            checkDialog();
            List<Member> members = new ArrayList<>();

            dialogAction = new MaterialDialog.Builder(this)
                    .title("Select a member to invite")
                    .negativeText("Cancel")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .adapter(new SelectMemberAdapter(this, members, this), null)
                    .show();
        }
    }

    private boolean isAdmin() {
        return twilioChannel != null && twilioChannel.getCreatedBy().equalsIgnoreCase(client.getMyUserInfo().getIdentity());
    }

    @OnClick(R.id.txt_leave_group)
    public void clickLeaveGroup() {
        checkDialog();
        dialogAction = new MaterialDialog.Builder(this)
                .title("Do you really want to leave this group?")
                .positiveText("Leave")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        currentChannel.leave(new Constants.StatusListener() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                                EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.GROUP_LEAVED));
                                setResult(ActivityResultCommon.RESULT_LEAVE_GROUP);
                                ChannelDetailActivity.this.finish();
                            }

                            @Override
                            public void onError(ErrorInfo errorInfo) {
                                dialog.dismiss();
                                MainApplication.get().showError(errorInfo);
                                super.onError(errorInfo);
                            }
                        });
                    }
                })
                .negativeText("Cancel")
                .build();
        dialogAction.show();
    }

    @OnClick(R.id.txt_remove_group)
    public void clickRemoveGroup() {
        if (!isAdmin()) return;
        checkDialog();
        dialogAction = new MaterialDialog.Builder(this)
                .title("Do you really want to remove this group?")
                .positiveText("Remove")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        currentChannel.destroy(new Constants.StatusListener() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                                EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.GROUP_REMOVED));
                                setResult(ActivityResultCommon.RESULT_REMOVE_GROUP);
                                ChannelDetailActivity.this.finish();
                            }

                            @Override
                            public void onError(ErrorInfo errorInfo) {
                                dialog.dismiss();
                                MainApplication.get().showError(errorInfo);
                                super.onError(errorInfo);
                            }
                        });
                    }
                })
                .negativeText("Cancel")
                .build();
        dialogAction.show();
    }

    @Override
    protected void onDestroy() {
        checkDialog();
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        if (subscriptionUser != null && !subscriptionUser.isUnsubscribed()) {
            subscriptionUser.unsubscribe();
        }
        super.onDestroy();
    }

    private void checkDialog() {
        if (dialogAction != null && dialogAction.isShowing()) {
            dialogAction.dismiss();
        }
    }

    private void showGroupInfo() {
        getSupportActionBar().setTitle(currentChannel.getFriendlyName());
        txtName.setText(currentChannel.getFriendlyName());
        try {
            String icon = currentChannel.getAttributes().optString("group_icon");
            if (icon != null && !icon.isEmpty())
                ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icon), imgGroup);
        } catch (Exception e) {}
    }

    private String getMemberName(String identity) {
        Member member = getMemberByIdentity(identity);
        if (member != null) {
            return getMemberName(member.getUserInfo());
        }
        return "";
    }

    private String getMemberName(UserInfo userInfo) {
        return (userInfo.getFriendlyName() != null && !userInfo.getFriendlyName().isEmpty())
                ?  userInfo.getFriendlyName() : userInfo.getIdentity();
    }

    private Member getMemberByIdentity(String identity) {
        final Members membersObject = currentChannel.getMembers();
        Member[] membersArray = membersObject.getMembers();
        if (membersArray != null && membersArray.length > 0) {
            for (Member member : membersArray) {
                if (member.getUserInfo().getIdentity().equalsIgnoreCase(identity)) {
                    return member;
                }
            }
        }
        return null;
    }

    private void loadMembers()
    {
        final Members membersObject = currentChannel.getMembers();

        Member[] membersArray = membersObject.getMembers();
        if (membersArray.length > 0) {
            members = new ArrayList<Member>(Arrays.asList(membersArray));
        }
        Collections.sort(members, new Comparator<Member>() {
            @Override
            public int compare(Member o1, Member o2) {
                if (o1.getUserInfo().getIdentity().equalsIgnoreCase(twilioChannel.getCreatedBy())) {
                    return -1;
                } else {
                    if (o1.getUserInfo().isOnline() && o2.getUserInfo().isOnline()) {
                        return 0;
                    } else if (o1.getUserInfo().isOnline() && !o2.getUserInfo().isOnline()) {
                        return -1;
                    } else if (!o1.getUserInfo().isOnline() && o2.getUserInfo().isOnline()) {
                        return 1;
                    } else if (o1.getUserInfo().isNotifiable()) {
                        return -1;
                    }
                }
                return o1.getUserInfo().getIdentity().compareTo(o2.getUserInfo().getIdentity());
            }
        });
        txtMemberCount.setText("Members: " + members.size() + " of " + MAX_MEMBER_SIZE);
        EasyAdapter<Member> adapterMember = new EasyAdapter<Member>(
                this, MemberViewHolder.class, members, new MemberViewHolder.OnMemberClickListener() {
            @Override
            public void onMemberClicked(Member member)
            {

            }
        });
        listMembers.setAdapter(adapterMember);
    }

    @Override
    public TwilioChannel getTwilioChannel() {
        return twilioChannel;
    }

    @Override
    public void selectIcon(String icon) {
        checkDialog();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("group_icon", icon);
            currentChannel.setAttributes(jsonObject, new Constants.StatusListener() {
                @Override
                public void onSuccess() {
                    currentChannel.synchronize(new Constants.CallbackListener<Channel>() {
                        @Override
                        public void onSuccess(Channel channel) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.CHANNELS_UPDATED));
                                }
                            });

                        }
                    });
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    super.onError(errorInfo);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icon), imgGroup);
    }

    @OnClick(R.id.img_group)
    public void clickSelectIcon() {
        if (!isAdmin()) return;
        try {
            checkDialog();
            dialogAction = new MaterialDialog.Builder(this)
                    .title("Select a group icon")
                    .negativeText("Cancel")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .adapter(new IconAdapter(this, getAssets().list("medical-icons"), this), new GridLayoutManager(this, 4))
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void selectMember(Member member) {

    }
}
