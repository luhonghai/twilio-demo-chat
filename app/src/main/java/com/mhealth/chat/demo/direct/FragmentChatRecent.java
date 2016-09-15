package com.mhealth.chat.demo.direct;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.adapter.ChatRecentAdapter2;
import com.mhealth.chat.demo.databinding.FragmentChatRecentBinding;
import com.mhealth.chat.demo.twilio.TwilioClient;
import com.twilio.ipmessaging.Channel;

import java.util.ArrayList;

/**
 * Created by leanh215 on 8/31/16.
 */
public class FragmentChatRecent extends Fragment {

    FragmentChatRecentBinding mBinding;
    TwilioClient mTwilioClient;

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
        mTwilioClient = MainApplication.get().getBasicClient();
        initUI();
        initChatRecentList();
    }

    private void initUI() {
        mBinding.toolbar.setNavigationOnClickListener(view1 -> getActivity().onBackPressed());
    }

    private void initChatRecentList() {
        ArrayList<Channel> recentChatList = new ArrayList<>();
        Channel[] channels = mTwilioClient.getIpMessagingClient().getChannels().getChannels();
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].getUniqueName().startsWith("chat_consult_")
                    && channels[i].getMembers().getMembers().length == 2) {
                recentChatList.add(channels[i]);
            }
        }

        mBinding.rvChatRecent.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.rvChatRecent.setAdapter(new ChatRecentAdapter2(getActivity(), recentChatList, channel -> openChatRecent(channel)));
    }

    private void openChatRecent(Channel channel) {
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, FragmentChatOneOne.getInstance(channel))
                .addToBackStack(null)
                .commit();
    }

}
