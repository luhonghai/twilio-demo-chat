package com.mhealth.chat.demo.view;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.util.DrawableUtils;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luhonghai on 9/1/16.
 */

public class UserInfoDialog {

    public interface UserInfoListener {
        void clickCall(Member member);
        void clickCancelCall(Member member);
        void clickChat(Member member);
    }

    private final Context context;

    private MaterialDialog dialog;

    @Bind(R.id.avatar)
    SimpleDraweeView imageView;

    @Bind(R.id.reachability)
    SimpleDraweeView reachabilityView;

    @Bind(R.id.txt_user_name)
    TextView txtName;

    @Bind(R.id.progress_bar)
    View progressBar;

    @Bind(R.id.text_action_fab)
    FloatingActionButton btnChat;

    @Bind(R.id.cancel_action_fab)
    FloatingActionButton btnCancel;

    @Bind(R.id.call_action_fab)
    FloatingActionButton btnCall;

    @OnClick(R.id.call_action_fab)
    public void clickCall(View view) {
        if (listener != null) {
            listener.clickCall((Member) view.getTag());
        }
        dismiss();
    }

    @OnClick(R.id.text_action_fab)
    public void clickChat(View view) {
        if (listener != null) {
            listener.clickChat((Member) view.getTag());
        }
    }

    @OnClick(R.id.cancel_action_fab)
    public void clickCancel(View view) {
        if (listener != null) {
            listener.clickCancelCall((Member) view.getTag());
        }
        dismiss();
    }

    private UserInfoListener listener;

    public UserInfoDialog(Context context) {
        this.context = context;
    }

    public void show(Member member,UserInfoListener listener) {
        show(member, listener, false);
    }

    public void show(Member member,UserInfoListener listener, boolean isInvite) {
        dismiss();
        this.listener = listener;
        View customView = LayoutInflater.from(context).inflate(R.layout.user_info_dialog, null);
        ButterKnife.bind(this, customView);
        btnChat.setTag(member);
        btnCancel.setTag(member);
        btnCall.setTag(member);
        if (isInvite) {
            btnChat.setVisibility(View.GONE);
            btnCancel.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnChat.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        }
        txtName.setText(getMemberName(member.getUserInfo()));
        DrawableUtils.fillUserAvatar(imageView, member);
        DrawableUtils.fillUserReachability(reachabilityView, member);
        dialog = new MaterialDialog.Builder(context)
                .cancelable(!isInvite)
                .customView(customView, false).show();
    }

    private String getMemberName(UserInfo userInfo) {
        return (userInfo.getFriendlyName() != null && !userInfo.getFriendlyName().isEmpty())
                ?  userInfo.getFriendlyName() : userInfo.getIdentity();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            ButterKnife.unbind(this);
        }
    }
}
