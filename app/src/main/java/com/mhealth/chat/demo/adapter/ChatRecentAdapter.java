package com.mhealth.chat.demo.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
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
public class ChatRecentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context mContext;
    ArrayList<Member> mRecentChatUsers;


    public ChatRecentAdapter(Context context, ArrayList<Member> recentChatUsers) {
        mContext = context;
        mRecentChatUsers = recentChatUsers;
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
        Member member = mRecentChatUsers.get(position);
        String avatarUrl = ChatUtils.getAvatarUrl(member);
        itemHolder.mBinding.imgAvatar.setImageURI(Uri.parse(avatarUrl));
        itemHolder.mBinding.tvUsername.setText(member.getUserInfo().getIdentity());

        if (member.getUserInfo().isOnline()) {
            itemHolder.mBinding.imgOnlineStatus.setColorFilter(mContext.getResources().getColor(R.color.colorPrimary));
        } else {
            itemHolder.mBinding.imgOnlineStatus.setColorFilter(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return mRecentChatUsers.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        ItemChatUserBinding mBinding;

        public ItemViewHolder(ItemChatUserBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }
}
