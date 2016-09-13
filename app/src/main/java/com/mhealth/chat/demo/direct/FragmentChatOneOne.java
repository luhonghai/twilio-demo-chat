package com.mhealth.chat.demo.direct;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.mhealth.chat.demo.databinding.FragmentChatOneOneBinding;
import com.mhealth.chat.demo.twilio.TwilioClient;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.ChannelListener;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leanh215 on 9/6/16.
 */
public class FragmentChatOneOne extends Fragment implements ChannelListener{

    private static final int LOAD_HISTORY_SIZE = 15;

    FragmentChatOneOneBinding mBinding;

    String mFriendId;
    String mFriendlyName;

    Member me;
    Member mFriend;

    TwilioClient mClient;

    String mChannelUniqueName;
    Channel mChannel;
    ChatMessageAdapter mChatMessageAdapter;
    boolean downloadingHistory;
    boolean noMoreHistory;

    Message lastMessageAdd;

    public static FragmentChatOneOne getInstance(String friendId) {
        FragmentChatOneOne fragmentChatOneOne = new FragmentChatOneOne();
        fragmentChatOneOne.mFriendId = friendId;
        return fragmentChatOneOne;
    }

    public static FragmentChatOneOne getInstance(String friendId, String friendlyName) {
        FragmentChatOneOne fragmentChatOneOne = new FragmentChatOneOne();
        fragmentChatOneOne.mFriendId = friendId;
        fragmentChatOneOne.mFriendlyName = friendlyName;
        return fragmentChatOneOne;
    }

    public static FragmentChatOneOne getInstance(Member friend) {
        FragmentChatOneOne fragmentChatOneOne = new FragmentChatOneOne();
        fragmentChatOneOne.mFriend = friend;
        fragmentChatOneOne.mFriendId = friend.getUserInfo().getIdentity();
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
        initUI();
        getPrivateChannaleUniqueName(mFriendId);
        connectToChannel();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat_one_one, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_get_me_last_seen) {
            MyLog.log("me last seen=" + me.getLastConsumedMessageIndex() + "; channel last seen=" + mChannel.getMessages().getLastConsumedMessageIndex());
            return true;
        } else if (item.getItemId() == R.id.action_set_me_last_seen) {
            MyLog.log("set last seen=" + lastMessageAdd.getMessageIndex());
            mChannel.getMessages().setLastConsumedMessageIndex(lastMessageAdd.getMessageIndex());
            return true;
        } else if (item.getItemId() == R.id.action_get_friend_last_seen) {
            MyLog.log("friend last seen=" + mFriend.getLastConsumedMessageIndex());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initUI() {
        // toolbar setting
        if (mFriendlyName != null) {
            mBinding.toolbar.setTitle(mFriendlyName);
        } else {
            mBinding.toolbar.setTitle(mFriendId);
        }

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
//        linearLayoutManager.setStackFromEnd(true);
//        linearLayoutManager.setReverseLayout(true);
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
//                MyLog.log("onScrollStateChanged; newState=" + newState + "; firstVisible=" + firstVisibleItem);

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

    private void connectToChannel() {
        MyLog.log("connectToChannel()");
        // 1. check channel exist in list channel
        // 1.1 exist => check status
        // 1.1.1 JOINED => do nothing
        // 1.1.2 INVITED => join
        // 1.2 not exist => create => join => invite

        // check channel existence
        mChannelUniqueName = getPrivateChannaleUniqueName(mFriendId);
        Channel[] channels = mClient.getIpMessagingClient().getChannels().getChannels();
        for (int i = 0; i < channels.length; i++) {
            Channel channel = channels[i];
             if (channel.getUniqueName().equals(mChannelUniqueName)) { // if channel exist
                 mChannel = channel;

                 if (channel.getStatus() == Channel.ChannelStatus.JOINED) { // if joined
                     // do nothing
                     MyLog.log("JOINED; " + mChannelUniqueName);

                     channel.setListener(FragmentChatOneOne.this);
                     if (channel.getMembers().getMembers().length == 1) { // if friend not joined yet
                         inviteFriend();
                     } else {
                         // both of two users JOINED this channel
                         checkMemberInChannelToInitChatMessageAdapter();
                         getMessageHistory(null, mChannel);
                     }
                 } else if (channel.getStatus() == Channel.ChannelStatus.INVITED) { // automatically join
                     MyLog.log("INVITED; " + mChannelUniqueName);
                     joinChannel(channel);
                 }
                 return;
             }
        }

        // if channel not exist, create channel + join + invite
        Map<String, Object> map = new HashMap<>();
        map.put("friendlyName", "PM:" + mClient.getIpMessagingClient().getMyUserInfo().getIdentity() + "+" + mFriendId);
        map.put("uniqueName", mChannelUniqueName);
        map.put("ChannelType", Channel.ChannelType.PRIVATE);

        mClient.getIpMessagingClient().getChannels().createChannel(map, new Constants.CreateChannelListener() {
            @Override
            public void onCreated(Channel channel) {
                MyLog.log("createChannel() --> onCreated() --> channelUniqueName=" + channel.getUniqueName());

                mChannel = channel;

                // join channel
                joinChannel(channel);

                // invite friend
                inviteFriend();
            }
        });
    }

    private void joinChannel(Channel channel) {
        MyLog.log("joinChannel(); " + channel.getUniqueName());
        channel.join(new Constants.StatusListener() {
            @Override
            public void onSuccess() {
                MyLog.log("channel.join() ==> onSuccess()");
                checkMemberInChannelToInitChatMessageAdapter();
                channel.setListener(FragmentChatOneOne.this);
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
            }
        });
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

                MyLog.log("getMessageHistory() ==> onSuccess() ==> size=" + messages.size());

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

    private void checkMemberInChannelToInitChatMessageAdapter() {
        MyLog.log("checkMemberInChannelToInitChatMessageAdapter()");
        Channel channel = mClient.getIpMessagingClient().getChannels().getChannelByUniqueName(mChannelUniqueName);

        if (channel.getMembers().getMembers().length == 2) {
            Member me;
            Member friend;
            if (channel.getMembers().getMembers()[0].getUserInfo().getIdentity().equals(mFriendId)) {
                friend = channel.getMembers().getMembers()[0];
                me = channel.getMembers().getMembers()[1];
            } else {
                friend = channel.getMembers().getMembers()[1];
                me = channel.getMembers().getMembers()[0];
            }

            mChatMessageAdapter = new ChatMessageAdapter(me, friend);
            this.me = me;
            this.mFriend = friend;

            mBinding.rvChatMessage.setAdapter(mChatMessageAdapter);

            String friendName = friend.getUserInfo().getFriendlyName();
            if (friendName.equals("")) {
                friendName = friend.getUserInfo().getIdentity();
            }

            mBinding.tvTypingIndicator.setText(friendName + " is typing...");
            mBinding.toolbar.setTitle(friendName);
            if (friend.getUserInfo().isOnline()) {
                mBinding.toolbar.setSubtitle("Online");
                mBinding.toolbar.setSubtitleTextColor(Color.WHITE);
            } else {
                mBinding.toolbar.setSubtitle("Offline");
                mBinding.toolbar.setSubtitleTextColor(Color.LTGRAY);
            }
        }
    }


    private String getPrivateChannaleUniqueName(String friendId) {
        String mId = mClient.getIpMessagingClient().getMyUserInfo().getIdentity();
        mChannelUniqueName = "pm_" + (mId.compareTo(friendId) > 0 ? mId + "_" + friendId : friendId + "_" + mId);
        MyLog.log("getPrivateChannaleUniqueName()=" + mChannelUniqueName);
        return mChannelUniqueName;
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

//                MyLog.log("setLastConsumedMessageIndex=" + message.getMessageIndex());
//                mChannel.getMessages().setLastConsumedMessageIndex(message.getMessageIndex());
//                mChatMessageAdapter.checkLastSeenMessage();

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
        checkMemberInChannelToInitChatMessageAdapter();
    }

    @Override
    public void onMemberChange(Member member) {
        MyLog.log("onMemberChange(); member=" + member.getUserInfo().getIdentity() + "; lastSeen=" + member.getLastConsumedMessageIndex());
        mChatMessageAdapter.updateMember(member);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
