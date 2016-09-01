package com.mhealth.chat.demo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.util.DrawableUtils;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luhonghai on 8/31/16.
 */

public class SelectMemberAdapter extends RecyclerView.Adapter<SelectMemberViewHolder> {

    private final List<Member> members;

    private final Context context;

    private final SelectMemberViewListener listener = new SelectMemberViewListener() {
        @Override
        public void selectMember(View view, Member member) {
            if (selectedMembers.contains(member)) {
                selectedMembers.remove(member);
                view.findViewById(R.id.img_selected).setVisibility(View.GONE);
            } else {
                selectedMembers.add(member);
                view.findViewById(R.id.img_selected).setVisibility(View.VISIBLE);
            }
        }
    };

    private final List<Member> selectedMembers = new ArrayList<>();

    public SelectMemberAdapter(Context context, List<Member> members) {
        this.members = members;
        this.context = context;
    }

    @Override
    public SelectMemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SelectMemberViewHolder(LayoutInflater.from(context).inflate(R.layout.member_item_layout,
                parent, false), listener);
    }

    @Override
    public void onBindViewHolder(SelectMemberViewHolder holder, int position) {
        Member member = members.get(position);
        fillUserReachability(holder.reachabilityView, member);
        fillUserAvatar(holder.imageView, member);
        holder.memberName.setText(getMemberName(member.getUserInfo()));
        holder.cardItem.setTag(member);
    }

    private String getMemberName(UserInfo userInfo) {
        return (userInfo.getFriendlyName() != null && !userInfo.getFriendlyName().isEmpty())
                ?  userInfo.getFriendlyName() : userInfo.getIdentity();
    }

    public List<Member> getSelectedMembers() {
        return selectedMembers;
    }

    private void fillUserAvatar(SimpleDraweeView avatarView, Member member)
    {
        JSONObject attributes = member.getUserInfo().getAttributes();
        String  avatar = (String)attributes.opt("avatar_url");
        avatarView.setImageURI(avatar);
    }

    public Context getContext() {
        return context;
    }

    private void fillUserReachability(SimpleDraweeView reachabilityView, Member member) {
        if (!MainApplication.get().getBasicClient().getIpMessagingClient().isReachabilityEnabled()) {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_block_black_24dp));
            reachabilityView.setColorFilter(getContext().getResources().getColor(R.color.colorOrange));
        } else if (member.getUserInfo().isOnline()) {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_online_black_24dp));
            reachabilityView.setColorFilter(getContext().getResources().getColor(R.color.colorPrimary));
        } else if (member.getUserInfo().isNotifiable()) {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_online_black_24dp));
            reachabilityView.setColorFilter(getContext().getResources().getColor(R.color.colorGray));
        } else {
            reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_lens_black_24dp));
            reachabilityView.setColorFilter(getContext().getResources().getColor(R.color.colorGray));
        }
    }

    @Override
    public int getItemCount() {
        return members != null ? members.size() : 0;
    }
}
