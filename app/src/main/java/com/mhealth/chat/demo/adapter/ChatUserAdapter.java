package com.mhealth.chat.demo.adapter;

import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.databinding.ItemChatUserBinding;
import com.mhealth.chat.demo.util.ChatUtils;
import com.twilio.ipmessaging.Member;

import java.util.ArrayList;

/**
 * Created by leanh215 on 9/5/16.
 */
public class ChatUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<Member> chatUsers;

    public ChatUserAdapter(ArrayList<Member> chatUsers) {
        this.chatUsers = chatUsers;

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemChatUserBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.item_chat_user, parent, false);
        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        Member member = chatUsers.get(position);
        String avatarUrl = ChatUtils.getAvatarUrl(member);
        itemHolder.mBinding.imgAvatar.setImageURI(Uri.parse(avatarUrl));
        itemHolder.mBinding.tvUsername.setText(member.getUserInfo().getIdentity());
    }

    @Override
    public int getItemCount() {
        return chatUsers.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        ItemChatUserBinding mBinding;

        public ItemViewHolder(ItemChatUserBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }
}
