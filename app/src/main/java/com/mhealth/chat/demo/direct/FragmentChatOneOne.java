package com.mhealth.chat.demo.direct;

import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.adapter.ChatMessageAdapter;
import com.mhealth.chat.demo.customview.SoftKeyboardHandledLinearLayout;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.databinding.FragmentChatOneOneBinding;
import com.mhealth.chat.demo.fcm.ChatConsultAcceptData;
import com.mhealth.chat.demo.fcm.ChatConsultCloseData;
import com.mhealth.chat.demo.fcm.ChatConsultRequestData;
import com.mhealth.chat.demo.fcm.FCMSenderService;
import com.mhealth.chat.demo.fcm.NotificationObject;
import com.mhealth.chat.demo.twilio.TwilioClient;
import com.mhealth.chat.demo.util.ChatUtils;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.ChannelListener;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by leanh215 on 9/6/16.
 */
public class FragmentChatOneOne extends Fragment implements ChannelListener{

    private static final int LOAD_HISTORY_SIZE = 15;

    FragmentChatOneOneBinding mBinding;

    ChatConsultRequestData mChatConsultRequestData;
    Channel mChannel;

    private String mFriendId;
    private String mFriendlyName;
    String mChannelUniqueName;

    boolean newChatConsultSession;
    TwilioClient mClient;

    ChatMessageAdapter mChatMessageAdapter;
    boolean downloadingHistory;
    boolean noMoreHistory;

    Message lastMessageAdd;

    ProgressDialog mProgressDialog;

    public static FragmentChatOneOne getInstance(ChatConsultRequestData request) {
        FragmentChatOneOne fragmentChatOneOne = new FragmentChatOneOne();
        fragmentChatOneOne.newChatConsultSession = true;
        fragmentChatOneOne.mChatConsultRequestData = request;
        return fragmentChatOneOne;
    }

    public static FragmentChatOneOne getInstance(Channel channel) {
        FragmentChatOneOne fragmentChatOneOne = new FragmentChatOneOne();
        fragmentChatOneOne.newChatConsultSession = false;
        fragmentChatOneOne.mChannel = channel;
        return fragmentChatOneOne;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_one_one, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mClient = MainApplication.get().getBasicClient();

        if (newChatConsultSession) {
            mChannelUniqueName = ChatUtils.generateChannelUniqueName(mChatConsultRequestData.getSessionId());
            mFriendId = mChatConsultRequestData.getSenderId();
            mFriendlyName = mChatConsultRequestData.getSenderFriendlyName();
            createChannel();
        } else {
            Member memberFriend = mChannel.getMembers().getMembers()[0].getUserInfo().getIdentity()
                    .equals(mClient.getIpMessagingClient().getMyUserInfo().getIdentity())
                    ? mChannel.getMembers().getMembers()[1] : mChannel.getMembers().getMembers()[0];
            mFriendId = memberFriend.getUserInfo().getIdentity();
            mFriendlyName = memberFriend.getUserInfo().getFriendlyName();
            openChannel();
        }

        initUI();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat_one_one, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_close_chat_consult) {
            closeChatConsult();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initUI() {
        mProgressDialog = new ProgressDialog(getActivity(), R.style.ProgressDialogDim);

        // toolbar setting
        mBinding.toolbar.setTitle(mFriendlyName);
        mBinding.tvTypingIndicator.setText(mFriendlyName + " is typing...");
        ((AppCompatActivity)getActivity()).setSupportActionBar(mBinding.toolbar);
        mBinding.toolbar.setNavigationOnClickListener(view1 -> getActivity().onBackPressed());

        // keyboard detector
        mBinding.llSoftkeyboardDetector.setOnSoftKeyboardVisibilityChangeListener(new SoftKeyboardHandledLinearLayout.SoftKeyboardVisibilityChangeListener() {
            @Override
            public void onSoftKeyboardShow() {
                MyLog.log("onSoftKeyboardShow()");
                if (mChatMessageAdapter != null) {
                    mBinding.rvChatMessage.scrollToPosition(mChatMessageAdapter.getItemCount()-1);
                }
            }

            @Override
            public void onSoftKeyboardHide() {
                MyLog.log("onSoftKeyboardHide()");
            }
        });

        // chat adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mBinding.rvChatMessage.setLayoutManager(linearLayoutManager);

        mBinding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (mChannel != null) {
                    mChannel.typing();
                }
            }
        });

        mBinding.btnSend.setOnClickListener(view -> sendMessage());

        mBinding.rvChatMessage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                int firstVisibleItem = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && firstVisibleItem == 0
                        && downloadingHistory == false
                        && noMoreHistory == false) {
                    getMessageHistory(mChatMessageAdapter.getTopMesage(), mChannel);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    private void createChannel() {
        MyLog.log("createChannel()");
        mBinding.tvWaitingMember.setText(String.format(getString(R.string.waiting_for_member_join_channel), mFriendlyName));
        mBinding.tvWaitingMember.setVisibility(View.VISIBLE);

        // if channel not exist, create channel + join + invite
        Map<String, Object> map = new HashMap<>();
        map.put("friendlyName", "Chat Consult");
        map.put("uniqueName", mChannelUniqueName);
        map.put("ChannelType", Channel.ChannelType.PRIVATE);

        mClient.getIpMessagingClient().getChannels().createChannel(map, new Constants.CreateChannelListener() {
            @Override
            public void onCreated(Channel channel) {
                MyLog.log("createChannel() --> onCreated() --> channelUniqueName=" + channel.getUniqueName());
                mChannel = channel;
                // join channel
                joinChannel();
            }
        });
    }

    private void joinChannel() {
        MyLog.log("joinChannel(); " + mChannel.getUniqueName());
        mChannel.join(new Constants.StatusListener() {
            @Override
            public void onSuccess() {
                MyLog.log("channel.join() ==> onSuccess()");
                mChannel.setListener(FragmentChatOneOne.this);

                // invite friend
                inviteFriend();
            }
        });

    }

    private void inviteFriend() {
        MyLog.log("inviteFriend(); " + mChannelUniqueName);
        Channel channel = mClient.getIpMessagingClient().getChannels().getChannelByUniqueName(mChannelUniqueName);
        channel.getMembers().inviteByIdentity(mFriendId, new Constants.StatusListener() {
            @Override
            public void onSuccess() {
                MyLog.log("inviteByIdentity() ==> onSuccess()");
                // send notification
                sendFCMAcceptNotification();
            }
        });
    }

    private void sendFCMAcceptNotification() {
        FCMSenderService.FCMRequest fcmRequest = new FCMSenderService.FCMRequest.Builder()
                .to(mChatConsultRequestData.getSenderToken())
                .title(getString(R.string.title_chat_consult_acceptance))
                .body(String.format(getString(R.string.content_chat_consult_acceptance),
                        mClient.getIpMessagingClient().getMyUserInfo().getFriendlyName()))
                .type(NotificationObject.Type.CHAT_CONSULT_ACCEPT)
                .data(new ChatConsultAcceptData(mClient.getIpMessagingClient().getMyUserInfo()).setChannelUniqueName(mChannelUniqueName))
                .build();

        FCMSenderService.send(fcmRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<FCMSenderService.FCMResponse>() {
                    @Override
                    public void call(FCMSenderService.FCMResponse fcmResponse) {
                        if (fcmResponse.getSuccess() == 0) {
                            MyLog.log("Send FCM Notification CHAT_CONSULT_ACCEPT failed");
                            getActivity().onBackPressed();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        MyLog.log("Send FCM Notification CHAT_CONSULT_ACCEPT failed");
                        getActivity().onBackPressed();
                    }
                });
    }

    private void openChannel() {
        MyLog.log("openChannel()");
        initChatMessageAdapter();
        getMessageHistory(null, mChannel);
        mChannel.setListener(this);

        // check closed channel
        boolean closedChatConsult = mChannel.getAttributes().optBoolean(TwilioChannel.ATTR_CLOSED_CHANNEL, false);
        if (closedChatConsult) {
            mBinding.layoutChatAction.setVisibility(View.GONE);
        }
    }

    private void sendMessage() {
        String messageBody = mBinding.etMessage.getText().toString().trim();
        if (!messageBody.equals("")) {
            mBinding.etMessage.setText("");

            Message message = mChannel.getMessages().createMessage(messageBody);
            mChatMessageAdapter.addSendingMessage(message);
            mBinding.rvChatMessage.scrollToPosition(mChatMessageAdapter.getItemCount()-1);
            mChannel.getMessages().sendMessage(message, new Constants.StatusListener() {
                @Override
                public void onSuccess() {
                    MyLog.log("sendMessage() ==> onSuccess()");
                }
            });
        }
    }

    private void getMessageHistory(Message topMessage, Channel channel) {
        MyLog.log("getMessageHistory(); topMessage=" + (topMessage == null ? "null" : topMessage.getMessageBody()));

        downloadingHistory = true; // flag download state
        mBinding.pbLoading.setVisibility(View.VISIBLE);

        Constants.CallbackListener<List<Message>> callbackListener = new Constants.CallbackListener<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingHistory = false; // flag download state
                        mBinding.pbLoading.setVisibility(View.GONE);

                        if (messages.size() > 0 && topMessage != null) {
                            messages.remove(messages.size()-1);
                        }

                        if (messages.size() == 0) { // if there are no more history messages
                            noMoreHistory = true;
                            return;
                        } else {
                            mChatMessageAdapter.addHistoryMessages(messages);
                            // if there is first history messages
                            if (topMessage == null) {
                                mBinding.rvChatMessage.scrollToPosition(messages.size()-1);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                MyLog.log("getMessageHistory() ==> onError(); error=" + errorInfo.getErrorText());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingHistory = false; // flag download state
                        mBinding.pbLoading.setVisibility(View.GONE);
                    }
                });

            }
        };

        if (topMessage == null) {
            channel.getMessages().getLastMessages(LOAD_HISTORY_SIZE, callbackListener);
        } else {
            channel.getMessages().getMessagesBefore(topMessage.getMessageIndex(), LOAD_HISTORY_SIZE, callbackListener);
        }

    }

    private void initChatMessageAdapter() {
        MyLog.log("initChatMessageAdapter()");

        if (mChannel.getMembers().getMembers().length == 2) {
            Member me;
            Member friend;
            if (mChannel.getMembers().getMembers()[0].getUserInfo().getIdentity().equals(mFriendId)) {
                friend = mChannel.getMembers().getMembers()[0];
                me = mChannel.getMembers().getMembers()[1];
            } else {
                friend = mChannel.getMembers().getMembers()[1];
                me = mChannel.getMembers().getMembers()[0];
            }

            mChatMessageAdapter = new ChatMessageAdapter(me, friend);
            mBinding.rvChatMessage.setAdapter(mChatMessageAdapter);

            if (friend.getUserInfo().isOnline()) {
                mBinding.toolbar.setSubtitle("Online");
                mBinding.toolbar.setSubtitleTextColor(Color.WHITE);
            } else {
                mBinding.toolbar.setSubtitle("Offline");
                mBinding.toolbar.setSubtitleTextColor(Color.LTGRAY);
            }
        }
    }

    private void closeChatConsult() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_close_chat_consult)
                .setMessage(R.string.content_close_chat_consult)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    closeChatConsultChannel();
                })
                .setNegativeButton("Cancel", (dialogInterface1, i1) -> dialogInterface1.dismiss())
                .show();
    }

    private void closeChatConsultChannel() {
        MyLog.log("closeChatConsultChannel() closed_channel=" + mChannel.getAttributes().opt(TwilioChannel.ATTR_CLOSED_CHANNEL));
        try {
            mChannel.setAttributes(mChannel.getAttributes().put(TwilioChannel.ATTR_CLOSED_CHANNEL, true), new Constants.StatusListener() {
                @Override
                public void onSuccess() {
                    MyLog.log("closeChatConsultChannel() ==> mChannel.setAttributes() ==> onSuccess()");

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sendFCMCloseChatConsultNotification();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendFCMCloseChatConsultNotification() {
        mProgressDialog.show();

        Member memberFriend = mChannel.getMembers().getMembers()[0].getUserInfo().getIdentity()
                .equals(mClient.getIpMessagingClient().getMyUserInfo().getIdentity())
                ? mChannel.getMembers().getMembers()[1] : mChannel.getMembers().getMembers()[0];

        FCMSenderService.FCMRequest fcmRequest = new FCMSenderService.FCMRequest.Builder()
                .to(memberFriend)
                .title(getString(R.string.title_chat_consult_closed))
                .body(String.format(getString(R.string.content_chat_consult_closed),
                        mClient.getIpMessagingClient().getMyUserInfo().getFriendlyName()))
                .type(NotificationObject.Type.CHAT_CONSULT_CLOSE)
                .data(new ChatConsultCloseData(mClient.getIpMessagingClient().getMyUserInfo(), mChannelUniqueName))
                .build();

        FCMSenderService.send(fcmRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<FCMSenderService.FCMResponse>() {
                    @Override
                    public void call(FCMSenderService.FCMResponse fcmResponse) {
                        mProgressDialog.dismiss();
                        getActivity().onBackPressed();

                        if (fcmResponse.getSuccess() == 0) {
                            MyLog.log("Send FCM Notification CHAT_CONSULT_CLOSE failed");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressDialog.dismiss();
                        getActivity().onBackPressed();

                        MyLog.log("Send FCM Notification CHAT_CONSULT_CLOSE failed");
                    }
                });
    }

    /**
     * ChannelListener implementation
     */

    @Override
    public void onMessageAdd(Message message) {
        lastMessageAdd = message;
        MyLog.log("onMessageAdd() messageIndex=" + message.getMessageIndex() + "; body=" + message.getMessageBody());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChatMessageAdapter.addNewMessage(message);
                mBinding.rvChatMessage.scrollToPosition(mChatMessageAdapter.getItemCount()-1);

                if (message.getAuthor().equals(mFriendId)) {
                    mChannel.getMessages().setLastConsumedMessageIndex(message.getMessageIndex());
                    MyLog.log("setLastConsumedMessageIndex=" + message.getMessageIndex());
                }
            }
        });
    }

    @Override
    public void onMessageChange(Message message) {
        MyLog.log("onMessageChange() message=" + message.getMessageBody());
    }

    @Override
    public void onMessageDelete(Message message) {

    }

    @Override
    public void onMemberJoin(Member member) {
        MyLog.log("onMemberJoin(); member=" + member.getUserInfo().getIdentity());
        mBinding.tvWaitingMember.setVisibility(View.GONE);
        initChatMessageAdapter();
    }

    @Override
    public void onMemberChange(Member member) {
        MyLog.log("onMemberChange(); member=" + member.getUserInfo().getIdentity() + "; lastSeen=" + member.getLastConsumedMessageIndex());

        if (getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChatMessageAdapter.updateMember(member);

                if (member.getUserInfo().isOnline()) {
                    mBinding.toolbar.setSubtitle("Online");
                    mBinding.toolbar.setSubtitleTextColor(Color.WHITE);
                } else {
                    mBinding.toolbar.setSubtitle("Offline");
                    mBinding.toolbar.setSubtitleTextColor(Color.LTGRAY);
                }
            }
        });
    }

    @Override
    public void onMemberDelete(Member member) {

    }

    @Override
    public void onAttributesChange(Map<String, String> map) {
        MyLog.log("onAttributesChange() map=" + map);
    }

    @Override
    public void onTypingStarted(Member member) {
        MyLog.log("onTypingStarted() member=" + member.getUserInfo().getIdentity());
        mBinding.tvTypingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTypingEnded(Member member) {
        MyLog.log("onTypingEnded() member=" + member.getUserInfo().getIdentity());
        mBinding.tvTypingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onSynchronizationChange(Channel channel) {
        MyLog.log("onSynchronizationChange(); channel=" + channel.getUniqueName());
    }
}
