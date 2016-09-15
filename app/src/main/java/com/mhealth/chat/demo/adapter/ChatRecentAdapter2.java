package com.mhealth.chat.demo.adapter;

import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.databinding.ItemChatUserBinding;
import com.mhealth.chat.demo.direct.MyLog;
import com.mhealth.chat.demo.util.ChatUtils;
import com.mhealth.chat.demo.util.DatetimeUtils;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by leanh215 on 9/14/16.
 */
public class ChatRecentAdapter2 extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener{

    Context mContext;
    ArrayList<Channel> mRecentChannels;
    ChatRecentListener mChatRecentListener;
    String mIdentity;

    public ChatRecentAdapter2(Context context, ArrayList<Channel> recentChannels, ChatRecentListener listener) {
        mContext = context;
        mRecentChannels = recentChannels;
        mChatRecentListener = listener;
        mIdentity = MainApplication.get().getBasicClient().getIpMessagingClient().getMyUserInfo().getIdentity();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemChatUserBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.item_chat_user, parent, false);
        return new ChatRecentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatRecentViewHolder chatRecentViewHolder = (ChatRecentViewHolder) holder;
        Channel channel = mRecentChannels.get(position);

        // set member
        Member member = channel.getMembers().getMembers()[0].getUserInfo().getIdentity().equals(mIdentity)
                ? channel.getMembers().getMembers()[1] : channel.getMembers().getMembers()[0];
        chatRecentViewHolder.mBinding.imgAvatar.setImageURI(ChatUtils.getAvatarUrl(member));
        chatRecentViewHolder.mBinding.tvUsername.setText(member.getUserInfo().getFriendlyName());
        if (member.getUserInfo().isOnline()) {
            chatRecentViewHolder.mBinding.imgOnlineStatus.setColorFilter(Color.LTGRAY);
        } else {
            chatRecentViewHolder.mBinding.imgOnlineStatus.setColorFilter(Color.parseColor("#68c7dd"));
        }

        // close state
        boolean closedChannel = channel.getAttributes().optBoolean(TwilioChannel.ATTR_CLOSED_CHANNEL, false);
        if (closedChannel) {
            chatRecentViewHolder.mBinding.imgClosedChannel.setVisibility(View.VISIBLE);
            chatRecentViewHolder.mBinding.tvNewMessageNumber.setVisibility(View.GONE);
        } else {
            chatRecentViewHolder.mBinding.imgClosedChannel.setVisibility(View.GONE);
            chatRecentViewHolder.mBinding.tvNewMessageNumber.setVisibility(View.VISIBLE);
        }

        // set last message
        channel.getMessages().getLastMessages(1, new Constants.CallbackListener<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (messages.size() > 0) {
                            chatRecentViewHolder.mBinding.tvLastMessage.setVisibility(View.VISIBLE);
                            chatRecentViewHolder.mBinding.tvTimeLastMessage.setVisibility(View.VISIBLE);

                            Message lastMessage = messages.get(0);
                            chatRecentViewHolder.mBinding.tvLastMessage.setText(lastMessage.getMessageBody());
                            chatRecentViewHolder.mBinding.tvTimeLastMessage.setText(DatetimeUtils.getTimestamp(lastMessage.getTimeStamp()));
                        } else {
                            chatRecentViewHolder.mBinding.tvLastMessage.setVisibility(View.GONE);
                            chatRecentViewHolder.mBinding.tvTimeLastMessage.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        Member memberMe = channel.getMembers().getMembers()[0].getUserInfo().getIdentity().equals(mIdentity)
                ? channel.getMembers().getMembers()[0] : channel.getMembers().getMembers()[1];
        MyLog.log("channel getLastConsumedMessageIndex()=" + channel.getMessages().getLastConsumedMessageIndex()
                + "; me getLastConsumedMessageIndex()=" + memberMe.getLastConsumedMessageIndex());
        long channelLastMsg = channel.getMessages().getLastConsumedMessageIndex() == null ? 0 : channel.getMessages().getLastConsumedMessageIndex();
        long myLastMsg = memberMe.getLastConsumedMessageIndex() == null ? 0 : memberMe.getLastConsumedMessageIndex();
        long newMessageNumber = channelLastMsg - myLastMsg;
        if (newMessageNumber > 0) {
            chatRecentViewHolder.mBinding.tvNewMessageNumber.setVisibility(View.VISIBLE);
            chatRecentViewHolder.mBinding.tvNewMessageNumber.setText(newMessageNumber + "");

            chatRecentViewHolder.mBinding.tvLastMessage.setTypeface(null, Typeface.BOLD);
        } else {
            chatRecentViewHolder.mBinding.tvNewMessageNumber.setVisibility(View.GONE);

            chatRecentViewHolder.mBinding.tvLastMessage.setTypeface(null, Typeface.NORMAL);
        }



        // on click
        chatRecentViewHolder.mBinding.getRoot().setTag(channel);
        chatRecentViewHolder.mBinding.getRoot().setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mRecentChannels.size();
    }

    @Override
    public void onClick(View view) {
        Channel channel = (Channel) view.getTag();
        mChatRecentListener.open(channel);
    }

    class ChatRecentViewHolder extends RecyclerView.ViewHolder {

        ItemChatUserBinding mBinding;

        public ChatRecentViewHolder(ItemChatUserBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }

    public interface ChatRecentListener {
        void open(Channel channel);
    }



}
