package com.mhealth.chat.demo.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.databinding.ItemChatMessageFriendBinding;
import com.mhealth.chat.demo.databinding.ItemChatMessageMeBinding;
import com.mhealth.chat.demo.direct.Const;
import com.mhealth.chat.demo.direct.MyLog;
import com.mhealth.chat.demo.util.ChatUtils;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by leanh215 on 9/6/16.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ME = 0x11;
    private static final int VIEW_TYPE_FRIEND = 0x12;

    Member me;
    Member friend;

    String avatarUrlFriend;

    ArrayList<Message> mMessages;
    private final Object lock = new Object();

    public ChatMessageAdapter(Member me, Member friend) {
        this.me = me;
        this.friend = friend;
//        avatarUrlMe = ChatUtils.getAvatarUrl(me);
        avatarUrlFriend = ChatUtils.getAvatarUrl(friend);
        mMessages = new ArrayList<>();
    }

    public void addHistoryMessages(List<Message> messages) {
        mMessages.addAll(0, messages);
        notifyDataSetChanged();
    }

    public void addNewMessage(Message message) {
        synchronized (lock) {
            MyLog.log("addNewMessage()  ==> message index=" + message.getMessageIndex());
            if (message.getAuthor().equals(me.getUserInfo().getIdentity())) {
                // this message comes from me => mark as SENT
                for (int i = mMessages.size()-1; i >0; i--) {
                    if (getMessageId(mMessages.get(i)) == getMessageId(message)) {
                        mMessages.set(i, message);
                        notifyDataSetChanged();
                        break;
                    }
                }
            } else {
                mMessages.add(message);
                notifyDataSetChanged();
            }
        }
    }

    public void addSendingMessage(Message message) {
        synchronized (lock) {

            long messageId = System.currentTimeMillis();
            Message sendingMessage = addMessageId(message, messageId);

            MyLog.log("addSendingMessage()  ==> message id=" + getMessageId(message));

            mMessages.add(sendingMessage);
            notifyDataSetChanged();
        }
    }

    public void updateMember(Member member) {
        if (member.getUserInfo().getIdentity().equals(me.getUserInfo().getIdentity())) {
            me = member;
            notifyDataSetChanged();
        } else if (member.getUserInfo().getIdentity().equals(friend.getUserInfo().getIdentity())) {
            friend = member;
            notifyDataSetChanged();
        }
    }

    private Message addMessageId(Message message, long id) {
        JSONObject attrs = message.getAttributes();
        try {
            attrs.put(Const.MSG_ID_KEY, id);
            message.setAttributes(attrs, new Constants.StatusListener() {
                @Override
                public void onSuccess() {
                    // TODO : question : anything need to do here?
                }
            });
            return message;
        } catch (JSONException e) {
            e.printStackTrace();
            return message;
        }
    }

    private long getMessageId(Message message) {
        JSONObject attrs = message.getAttributes();
        if (attrs.has(Const.MSG_ID_KEY)) {
            try {
                return attrs.getLong(Const.MSG_ID_KEY);
            } catch (JSONException e) {
                e.printStackTrace();
                return Const.MSG_ID_NOT_SET;
            }
        }
        return Const.MSG_ID_NOT_SET;
    }

    public Message getTopMesage() {
        return mMessages.size() > 0 ? mMessages.get(0) : null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ME) {
            ItemChatMessageMeBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                    R.layout.item_chat_message_me, parent, false);
            return new MeMessageViewHolder(binding);
        } else {
            ItemChatMessageFriendBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                    R.layout.item_chat_message_friend, parent, false);
            return new FriendMessageViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = mMessages.get(position);
        if (holder instanceof MeMessageViewHolder) {
            MeMessageViewHolder itemHolder = (MeMessageViewHolder) holder;
            itemHolder.mBinding.tvMessageBody.setText(message.getMessageBody());

            // check send status
            if (message.getSid() != null) { // this message is sent
                if (position == getItemCount()-1) { // show status icon if this is last message from me
                    itemHolder.mBinding.imgReadStatus.setImageResource(R.drawable.ic_msg_sent_16dp);
                } else { // hide status icon
                    itemHolder.mBinding.imgReadStatus.setImageResource(R.drawable.ic_msg_blank_16p);
                }
            } else {
                itemHolder.mBinding.imgReadStatus.setImageResource(R.drawable.ic_msg_sending_16dp);
            }

            // check read status
            if (friend.getLastConsumedMessageIndex() != null && friend.getLastConsumedMessageIndex() == message.getMessageIndex()) {
                MyLog.log("OK; friend reached messagebody=" + message.getMessageBody());
                itemHolder.mBinding.imgReadStatus.setImageURI(avatarUrlFriend);
            }

        } else if (holder instanceof FriendMessageViewHolder) {
            FriendMessageViewHolder itemHolder = (FriendMessageViewHolder) holder;
            itemHolder.mBinding.imgAvatar.setImageURI(avatarUrlFriend);
            itemHolder.mBinding.tvMessageBody.setText(message.getMessageBody());

            if (position > 0 && mMessages.get(position-1).getAuthor().equals(message.getAuthor())) {
                itemHolder.mBinding.imgAvatar.setVisibility(View.INVISIBLE);
            } else {
                itemHolder.mBinding.imgAvatar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = mMessages.get(position);
        if (message.getAuthor().equals("") ||  message.getAuthor().equals(me.getUserInfo().getIdentity())) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_FRIEND;
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    class MeMessageViewHolder extends RecyclerView.ViewHolder {
        ItemChatMessageMeBinding mBinding;
        public MeMessageViewHolder(ItemChatMessageMeBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }

    class FriendMessageViewHolder extends RecyclerView.ViewHolder {
        ItemChatMessageFriendBinding mBinding;
        public FriendMessageViewHolder(ItemChatMessageFriendBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }


}
