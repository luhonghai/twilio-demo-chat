package com.mhealth.chat.demo.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.databinding.ItemChatContactBinding;
import com.mhealth.chat.demo.util.ChatUtils;
import com.twilio.ipmessaging.Member;

import java.util.ArrayList;

/**
 * Created by leanh215 on 9/14/16.
 */
public class ChatContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context mContext;
    ArrayList<Member> mContacts;

    public ChatContactAdapter(Context context,ArrayList<Member> contacts) {
        mContext = context;
        mContacts = contacts;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemChatContactBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.item_chat_contact, parent, false);
        return new MemberViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Member member = mContacts.get(position);
        MemberViewHolder memberViewHolder = (MemberViewHolder) holder;
        memberViewHolder.mBinding.imgAvatar.setImageURI(ChatUtils.getAvatarUrl(member));
        if (member.getUserInfo().isOnline()) {
            memberViewHolder.mBinding.imgOnlineStatus.setColorFilter(mContext.getResources().getColor(R.color.colorPrimary));
        } else {
            memberViewHolder.mBinding.imgOnlineStatus.setColorFilter(Color.LTGRAY);
        }

        memberViewHolder.mBinding.tvUsername.setText(member.getUserInfo().getFriendlyName());
        memberViewHolder.mBinding.tvUserId.setText(member.getUserInfo().getIdentity());
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {

        ItemChatContactBinding mBinding;

        public MemberViewHolder(ItemChatContactBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

    }

}
