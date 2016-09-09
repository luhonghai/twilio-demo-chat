package com.mhealth.chat.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.adapter.IconAdapter;
import com.mhealth.chat.demo.adapter.IconViewListener;
import com.mhealth.chat.demo.adapter.SelectMemberAdapter;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.data.TwilioUser;
import com.mhealth.chat.demo.event.ChannelEvent;
import com.mhealth.chat.demo.twilio.TwilioService;
import com.mhealth.chat.demo.view.UserInfoDialog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.ChannelListener;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Members;
import com.twilio.ipmessaging.Message;
import com.twilio.ipmessaging.UserInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
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
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.ribot.easyadapter.EasyAdapter;

public class ChannelDetailActivity extends AppCompatActivity implements MemberViewHolder.TwilioChannelView,IconViewListener,
        ChannelListener {

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

    private UserInfoDialog userInfoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);
        ButterKnife.bind(this);
        userInfoDialog = new UserInfoDialog(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(Channel.class.getName())) {
            currentChannel = (Channel) bundle.get(Channel.class.getName());
            if (currentChannel == null) finish();
            currentChannel.setListener(this);
            client = MainApplication.get().getBasicClient().getIpMessagingClient();
            twilioChannel = MainApplication.get().getChannelDataPreference().get(currentChannel.getSid());
            showGroupInfo();
            if (twilioChannel == null) {
                loadChannelInfo(false);
                subscriptionUser = TwilioService.getInstance().listUsers(1000, 0)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(new Action1<TwilioService.TwilioUsersResponse>() {
                            @Override
                            public void call(TwilioService.TwilioUsersResponse twilioUsersResponse) {
                                ChannelDetailActivity.this.twilioUsers = twilioUsersResponse.getUsers();
                                try {
                                    Log.d("", "Receive members list size " + ChannelDetailActivity.this.twilioUsers.size());
                                } catch (Exception e) {
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        });
            } else {
                showAdditionalInfo();
            }
        } else {
            this.finish();
        }
    }

    private void loadChannelInfo(final boolean notify) {
        subscription = TwilioService.getInstance().getChannel(currentChannel.getSid())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<TwilioChannel>() {
                    @Override
                    public void call(TwilioChannel twilioChannel) {
                        ChannelDetailActivity.this.twilioChannel = twilioChannel;
                        MainApplication.get().getChannelDataPreference().put(twilioChannel);
                        showAdditionalInfo();
                        showGroupInfo();
                        if (notify) {
                            EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.CHANNELS_UPDATED));
                            EventBus.getDefault().post(new ChannelEvent(ChannelEvent.Type.UPDATED, currentChannel));
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                        loadMembers();
                    }
                });
    }

    @OnClick(R.id.channel_friendly_name)
    public void clickGroupName() {
        if (isAdmin()) {
            new MaterialDialog.Builder(this)
                    .title("Rename group")
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input("New group name", txtName.getText().toString(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull final MaterialDialog dialog, final CharSequence input) {
                            final String name = input.toString();
                            if (name.isEmpty()) return;
                            currentChannel.setFriendlyName(name, new Constants.StatusListener() {
                                @Override
                                public void onSuccess() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtName.setText(name);
                                            dialog.dismiss();
                                            loadChannelInfo(true);
                                        }
                                    });
                                }

                                @Override
                                public void onError(ErrorInfo errorInfo) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.dismiss();
                                        }
                                    });
                                    MainApplication.get().showError(errorInfo);
                                }
                            });
                        }
                    })
                    .positiveText("Rename")
                    .negativeText("Cancel")
                    .show();
        }
    }

    private void showAdditionalInfo() {
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


    private boolean checkContainMember(List<Member> members, Member member) {
        if (members != null && members.size() > 0) {
            for (Member m : members) {
                if (member.getUserInfo().getIdentity().equalsIgnoreCase(m.getUserInfo().getIdentity())) return true;
            }
        }
        return false;
    }

    @OnClick(R.id.add_member)
    public void clickAddMember() {
        if ((members != null && members.size() > 0)
                && (isAdmin() || currentChannel.getType() == Channel.ChannelType.PUBLIC)) {
            checkDialog();
            Channel[] channels = client.getChannels().getChannels();
            final List<Member> inviteMembers = new ArrayList<>();
            if (channels != null && channels.length > 0) {
                for (Channel channel : channels) {
                    if (channel.getSid().equalsIgnoreCase(currentChannel.getSid())
                            || channel.getStatus() != Channel.ChannelStatus.JOINED)
                        continue;
                    List<Member> list = Arrays.asList(channel.getMembers().getMembers());
                    for (Member member : list) {
                        if (!checkContainMember(members,member) && !checkContainMember(inviteMembers,member)) {
                            inviteMembers.add(member);
                        }
                    }
                }
                if (inviteMembers.size() > 0) {
                    final SelectMemberAdapter adapter = new SelectMemberAdapter(this, inviteMembers);
                    dialogAction = new MaterialDialog.Builder(this)
                            .title("Select members to invite")
                            .negativeText("Cancel")
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveText("Invite")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    invite(adapter.getSelectedMembers());
                                }
                            })
                            .adapter(adapter, null)
                            .show();
                } else {
                    new MaterialDialog.Builder(this)
                            .title("Enter identity to invite")
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input("", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, final CharSequence input) {
                                    final String identity = input.toString();
                                    if (identity.isEmpty()) return;
                                    currentChannel.getMembers().inviteByIdentity(identity, new Constants.StatusListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d("Invite member", "onSuccess: " + identity + " channel " + currentChannel.getFriendlyName());
                                        }

                                        @Override
                                        public void onError(ErrorInfo errorInfo) {
                                            MainApplication.get().showError(errorInfo);
                                            super.onError(errorInfo);
                                        }
                                    });
                                }
                            })
                            .positiveText("Invite")
                            .show();
                }
            }
        }
    }

    private void invite(final List<Member> members) {
        checkDialog();
        if (members == null || members.size() == 0) return;
        dialogAction = new MaterialDialog.Builder(this)
                .title("Invite " + members.size() + " member" + (members.size() > 1 ? "s" : "") + "?")
                .negativeText("Cancel")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .positiveText("Invite")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        for (final Member member : members) {
                            currentChannel.getMembers().inviteByIdentity(member.getUserInfo().getIdentity(), new Constants.StatusListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d("Invite member", "onSuccess: " + member.getUserInfo().getIdentity() + " channel " + currentChannel.getFriendlyName());
                                }
                            });
                        }
                    }
                })
                .show();
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
        if (userInfoDialog != null) userInfoDialog.dismiss();
        super.onDestroy();
    }

    private void checkDialog() {
        if (dialogAction != null && dialogAction.isShowing()) {
            dialogAction.dismiss();
        }
    }

    private void showGroupInfo() {
        if (twilioChannel != null) {
            getSupportActionBar().setTitle(twilioChannel.getFriendlyName());
            txtName.setText(twilioChannel.getFriendlyName());
            TwilioChannel.Attribute attrObject = twilioChannel.getAttributeObject();
            if (attrObject != null) {
                ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(attrObject.getIcon()), imgGroup);
            }
        } else {
            getSupportActionBar().setTitle(currentChannel.getFriendlyName());
            txtName.setText(currentChannel.getFriendlyName());
            try {
                String icon = currentChannel.getAttributes().optString("group_icon");
                if (icon != null && !icon.isEmpty())
                    ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icon), imgGroup);
            } catch (Exception e) {}
        }

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
                userInfoDialog.show(member, new UserInfoDialog.UserInfoListener() {
                    @Override
                    public void clickCall(Member member) {
                        Intent intent = new Intent(ChannelDetailActivity.this, ConversationActivity.class);
                        intent.putExtra(ConversationActivity.VIDEO_ACTION, ConversationActivity.ACTION_CALL);
                        intent.putExtra(ConversationActivity.TARGET_IDENTITY, member.getUserInfo().getIdentity());
                        startActivity(intent);
                    }

                    @Override
                    public void clickCancelCall(Member member) {

                    }

                    @Override
                    public void clickChat(Member member) {

                    }
                });
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
                        public void onSuccess(final Channel channel) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.CHANNELS_UPDATED));
                                    EventBus.getDefault().post(new ChannelEvent(ChannelEvent.Type.UPDATED, channel));
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
        if (twilioChannel != null) {
            twilioChannel.getAttributeObject().setIcon(icon);
            MainApplication.get().getChannelDataPreference().put(twilioChannel);
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
    public void onMessageAdd(Message message) {

    }

    @Override
    public void onMessageChange(Message message) {

    }

    @Override
    public void onMessageDelete(Message message) {

    }

    @Override
    public void onMemberJoin(Member member) {
        loadMembers();
    }

    @Override
    public void onMemberChange(Member member) {
        loadMembers();
    }

    @Override
    public void onMemberDelete(Member member) {
        loadMembers();
    }

    @Override
    public void onAttributesChange(Map<String, String> map) {

    }

    @Override
    public void onTypingStarted(Member member) {

    }

    @Override
    public void onTypingEnded(Member member) {

    }

    @Override
    public void onSynchronizationChange(Channel channel) {

    }
}
