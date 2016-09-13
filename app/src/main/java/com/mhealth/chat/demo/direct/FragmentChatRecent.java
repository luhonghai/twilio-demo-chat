package com.mhealth.chat.demo.direct;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.adapter.ChatRecentAdapter;
import com.mhealth.chat.demo.databinding.FragmentChatRecentBinding;
import com.mhealth.chat.demo.twilio.TwilioClient;
import com.mhealth.chat.demo.util.ChatUtils;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import java.util.ArrayList;

/**
 * Created by leanh215 on 8/31/16.
 */
public class FragmentChatRecent extends Fragment {

    FragmentChatRecentBinding mBinding;
    TwilioClient mClient;

    public static FragmentChatRecent getInstance() {
        FragmentChatRecent fragmentChatRecent = new FragmentChatRecent();
        return fragmentChatRecent;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_recent, container, false);
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
        mBinding.toolbar.setTitle("My Chat");
        ((AppCompatActivity)getActivity()).setSupportActionBar(mBinding.toolbar);
        mBinding.toolbar.setNavigationOnClickListener(view1 -> getActivity().onBackPressed());

        mBinding.btnLily.setOnClickListener(view1 -> {
            mBinding.etFriendId.setText("lily.van@manadr.com");
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, FragmentChatOneOne.getInstance("lily.van@manadr.com"))
                    .addToBackStack(null)
                    .commit();
        });

        mBinding.btnAlex.setOnClickListener(view1 -> {
            mBinding.etFriendId.setText("alex.lee@manadr.com");
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, FragmentChatOneOne.getInstance("alex.lee@manadr.com"))
                    .addToBackStack(null)
                    .commit();
        });






    }

    private void initChatList() {
        mBinding.rvChatUser.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.rvChatUser.setAdapter(new ChatRecentAdapter(getActivity(), getChatUsers()));
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

}
