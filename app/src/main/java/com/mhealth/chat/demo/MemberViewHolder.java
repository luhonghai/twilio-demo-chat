package com.mhealth.chat.demo;

import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.util.DrawableUtils;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import uk.co.ribot.easyadapter.ItemViewHolder;
import uk.co.ribot.easyadapter.PositionInfo;
import uk.co.ribot.easyadapter.annotations.LayoutId;
import uk.co.ribot.easyadapter.annotations.ViewId;

@LayoutId(R.layout.member_item_layout)
public class MemberViewHolder extends ItemViewHolder<Member>
{

    public interface TwilioChannelView {
        TwilioChannel getTwilioChannel();
    }

    @ViewId(R.id.txt_name)
    TextView memberName;

    @ViewId(R.id.view_admin)
    View viewAdmin;

    @ViewId(R.id.avatar)
    SimpleDraweeView imageView;

    @ViewId(R.id.reachability)
    SimpleDraweeView reachabilityView;

    View view;

    public MemberViewHolder(View view)
    {
        super(view);
        this.view = view;
    }

    @Override
    public void onSetListeners()
    {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                OnMemberClickListener listener = getListener(OnMemberClickListener.class);
                if (listener != null) {
                    listener.onMemberClicked(getItem());
                }
            }
        });
    }

    public interface OnMemberClickListener {
        void onMemberClicked(Member member);
    }

    @Override
    public void onSetValues(Member member, PositionInfo arg1)
    {
        fillUserReachability(reachabilityView, member);
        fillUserAvatar(imageView, member);
        memberName.setText(getMemberName(member.getUserInfo()));
        if (view.getContext() instanceof TwilioChannelView) {
            TwilioChannelView twilioChannelView = (TwilioChannelView) view.getContext();
            TwilioChannel twilioChannel = twilioChannelView.getTwilioChannel();
            if (twilioChannel != null && twilioChannel.getCreatedBy().equalsIgnoreCase(member.getUserInfo().getIdentity())) {
                viewAdmin.setVisibility(View.VISIBLE);
            } else {
                viewAdmin.setVisibility(View.GONE);
            }
        }
    }

    private String getMemberName(UserInfo userInfo) {
        return (userInfo.getFriendlyName() != null && !userInfo.getFriendlyName().isEmpty())
                ?  userInfo.getFriendlyName() : userInfo.getIdentity();
    }

    private void fillUserAvatar(SimpleDraweeView avatarView, Member member)
    {
        JSONObject attributes = member.getUserInfo().getAttributes();
        String  avatar = (String)attributes.opt("avatar_url");
        avatarView.setImageURI(avatar);
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
}
