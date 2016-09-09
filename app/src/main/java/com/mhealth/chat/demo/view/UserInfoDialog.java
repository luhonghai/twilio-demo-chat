package com.mhealth.chat.demo.view;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.data.ChatConsultSession;
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

    public enum Type {
        INFO,
        INVITE,
        CHAT_CONSULT
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

    @Bind(R.id.txt_message)
    TextView txtMessage;

    @OnClick(R.id.call_action_fab)
    public void clickCall(View view) {
        if (listener != null) {
            listener.clickCall((Member) view.getTag());
        }
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
    }

    private UserInfoListener listener;

    public UserInfoDialog(Context context) {
        this.context = context;
    }

    public void show(Member member,UserInfoListener listener) {
        show(member, listener, Type.INFO);
    }

    public void showProgress() {
        if (dialog != null && dialog.isShowing()) {
            txtMessage.setText("Preparing for chat session ...");
            btnCall.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
            btnChat.setVisibility(View.GONE);
        }
    }

    public void show(Member member,UserInfoListener listener, Type type) {
        dismiss();
        this.listener = listener;
        View customView = LayoutInflater.from(context).inflate(R.layout.user_info_dialog, null);
        ButterKnife.bind(this, customView);
        btnChat.setTag(member);
        btnCancel.setTag(member);
        btnCall.setTag(member);
        switch (type) {
            case INVITE:
                btnChat.setVisibility(View.GONE);
                btnCall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_white_24px));
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_end_white_24px));
                progressBar.setVisibility(View.VISIBLE);
                txtMessage.setText("Incoming call ...");
                break;
            case INFO:
                btnChat.setVisibility(View.VISIBLE);
                btnCall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_videocam_white_24dp));
                btnCancel.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                break;
            case CHAT_CONSULT:
                txtMessage.setText("New chat consult request ...");
                btnChat.setVisibility(View.GONE);
                btnCall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done_white_24dp));
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_not_interested_white_24dp));
                progressBar.setVisibility(View.VISIBLE);
                break;
        }
        if (type == Type.CHAT_CONSULT) {
            ChatConsultSession session = ChatConsultSession.decodeChannelName(member.getSid());
            if (session != null) {
                txtName.setText(session.getPatientName());
                reachabilityView.setImageURI(DrawableUtils.getResourceURI(R.drawable.ic_online_black_24dp));
                reachabilityView.setColorFilter(reachabilityView.getContext().getResources().getColor(R.color.colorPrimary));
                imageView.setImageURI(session.getPatientAvatar());
            }
        } else {
            txtName.setText(getMemberName(member.getUserInfo()));
            DrawableUtils.fillUserAvatar(imageView, member);
            DrawableUtils.fillUserReachability(reachabilityView, member);
        }
        dialog = new MaterialDialog.Builder(context)
                .cancelable(type == Type.INFO)
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
