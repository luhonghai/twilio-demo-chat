package com.mhealth.chat.demo.direct;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mhealth.chat.demo.BasicIPMessagingClient;
import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.adapter.ChatUserAdapter;
import com.mhealth.chat.demo.databinding.FragmentChatUserListBinding;
import com.mhealth.chat.demo.util.ChatUtils;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Channels;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by leanh215 on 8/31/16.
 */
public class FragmentChatUserList extends Fragment {

    FragmentChatUserListBinding mBinding;
    BasicIPMessagingClient mClient;

    String mChannelUniqueName;
    Channel mChannel;

    public static FragmentChatUserList getInstance() {
        FragmentChatUserList fragmentChatUserList = new FragmentChatUserList();
        return fragmentChatUserList;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_user_list, container, false);
        mBinding.getRoot().requestFocus();
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mClient = MainApplication.get().getBasicClient();

        initUI();
        initChatList();
    }

    private void initUI() {
        mBinding.btnLily.setOnClickListener(view1 -> {
            mBinding.etFriendId.setText("lily.van@manadr.com");
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, FragmentChatWithFriend.getInstance("lily.van@manadr.com"))
                    .addToBackStack(null)
                    .commit();
        });
        mBinding.btnAlex.setOnClickListener(view1 -> {
            mBinding.etFriendId.setText("alex.lee@manadr.com");
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, FragmentChatWithFriend.getInstance("alex.lee@manadr.com"))
                    .addToBackStack(null)
                    .commit();
        });

        mBinding.btnJoin.setOnClickListener(view -> checkChannelExist());
        mBinding.btnInvite.setOnClickListener(view -> inviteFriend());
        mBinding.btnRemove.setOnClickListener(view -> removeFriend());
        mBinding.btnGetListMembers.setOnClickListener(view -> getListMemebers());
        mBinding.btnGetListChannels.setOnClickListener(view -> getListChannels());
    }

    private void initChatList() {
        mBinding.rvChatUser.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.rvChatUser.setAdapter(new ChatUserAdapter(getChatUsers()));
    }

    private ArrayList<Member> getChatUsers() {
        ArrayList<Member> members = new ArrayList<>();
        UserInfo mUserInfo = mClient.getIpMessagingClient().getMyUserInfo();
        Channel[] channels = mClient.getIpMessagingClient().getChannels().getChannels();
        for (int i = 0; i < channels.length; i++) {
            Channel channel = channels[i];
            if (channel.getUniqueName().startsWith("pm_")) {
                Member chatUser = ChatUtils.getChatUser(mUserInfo, channels[i]);
                if (chatUser != null) {
                    members.add(chatUser);
                }
            }
        }
        return members;
    }

    private void chatWith(String friendId) {

    }

    private void getPrivateChannaleUniqueName(String friendId) {
        String mId = mClient.getIpMessagingClient().getMyUserInfo().getIdentity();
        mChannelUniqueName = "pm_" + (mId.compareTo(friendId) > 0 ? mId + "_" + friendId : friendId + "_" + mId);
        Log.e("stk", "uniqueChannelName=" + mChannelUniqueName);
    }

    private void checkChannelExist() {
        String id1 = mClient.getIpMessagingClient().getMyUserInfo().getIdentity();
        String id2 = mBinding.etFriendId.getText().toString().trim();
        ;

        mChannelUniqueName = "pm_" + (id1.compareTo(id2) > 0 ? id1 + "_" + id2 : id2 + "_" + id1);
        Log.e("stk", "uniqueChannelName=" + mChannelUniqueName);

        // check if channel existence
        Channel channel = mClient.getIpMessagingClient().getChannels().getChannelByUniqueName(mChannelUniqueName);
        if (channel == null) {
            createChannel(mChannelUniqueName);
        } else {
            joinChannel(channel);
        }
    }

    private void createChannel(String uniqueChannelName) {
        MyLog.log("createChannel()=" + uniqueChannelName);

        String id1 = mClient.getIpMessagingClient().getMyUserInfo().getIdentity();
        String id2 = mBinding.etFriendId.getText().toString().trim();
        String friendlyName = "FN: " + (id1.compareTo(id2) > 0 ? id1 + "-" + id2 : id2 + "-" + id1);

        Map<String, Object> map = new HashMap<>();
        map.put("friendlyName", friendlyName);
        map.put("uniqueName", uniqueChannelName);
        map.put("ChannelType", Channel.ChannelType.PRIVATE);

        mClient.getIpMessagingClient().getChannels().createChannel(map, new Constants.CreateChannelListener() {
            @Override
            public void onCreated(Channel channel) {
                MyLog.log("createChannel() --> onCreated() --> channelUniqueName=" + channel.getUniqueName());
                joinChannel(channel);
            }
        });
    }

    private void joinChannel(Channel channel) {
        mChannel = channel;
        MyLog.log("joinChannel()=" + channel.getUniqueName());

        channel.join(new Constants.StatusListener() {
            @Override
            public void onSuccess() {
                MyLog.log("channel.join() --> onSuccess()");
            }
        });
    }

    private void inviteFriend() {
        String friendId = mBinding.etFriendId.getText().toString().trim();
        mChannel.getMembers().inviteByIdentity(friendId, new Constants.StatusListener() {
            @Override
            public void onSuccess() {
                MyLog.log("mChannel inviteByIdentity() --> onSuccess()");
            }
        });
    }

    private void removeFriend() {
        String friendId = mBinding.etFriendId.getText().toString().trim();
        Member[] memberList = mChannel.getMembers().getMembers();
        for (int i = 0; i < memberList.length; i++) {
            if (memberList[i].getUserInfo().getIdentity().equals(friendId)) {

                mChannel.getMembers().removeMember(memberList[i], new Constants.StatusListener() {
                    @Override
                    public void onSuccess() {
                        MyLog.log("mChannel removeMember() --> onSuccess()");
                    }
                });
                break;
            }
        }
    }

    private void getListMemebers() {
        Member[] memberList = mChannel.getMembers().getMembers();
        MyLog.log("member size=" + memberList.length);
        for (int i = 0; i < memberList.length; i++) {
            MyLog.log("member[" + i + "]" + memberList[i].getUserInfo().getIdentity());
        }
    }

    private void getListChannels() {
        Channels channels = mClient.getIpMessagingClient().getChannels();
        Channel[] channelList = channels.getChannels();
        MyLog.log("channelSize=" + channelList.length);
        for (int i = 0; i < channelList.length; i++) {
            Channel channel = channelList[i];
            if (channel.getStatus() == Channel.ChannelStatus.NOT_PARTICIPATING) {
                MyLog.log("channel[" + i + "]=" + channel.getUniqueName() + ";" + channel.getFriendlyName()
                        + "; status=" + channel.getStatus());
            } else {
                MyLog.log("channel[" + i + "]=" + channel.getUniqueName() + ";" + channel.getFriendlyName()
                        + "; status=" + channel.getStatus()
                        + "; member size=" + channel.getMembers().getMembers().length
                );
            }
        }
    }


}
