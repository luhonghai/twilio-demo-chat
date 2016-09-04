package com.mhealth.chat.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.event.ChannelEvent;
import com.mhealth.chat.demo.view.UserInfoDialog;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.IncomingInvite;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.Member;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by luhonghai on 9/1/16.
 */

public class BaseActivity extends AppCompatActivity {

    protected Logger logger = Logger.getLogger(this.getClass());

    private UserInfoDialog callInviteDialog;

    private MaterialDialog incomingChannelInvite;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        callInviteDialog = new UserInfoDialog(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.get().setInApplication(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.get().setInApplication(false);
    }


    protected Conversation getCurrentConversation() {
        return null;
    }

    @Override
    protected void onDestroy() {
        if (callInviteDialog != null) callInviteDialog.dismiss();
        if (incomingChannelInvite != null) incomingChannelInvite.dismiss();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe
    public void onChannelEvent(ChannelEvent event) {
        if (event.getType() == ChannelEvent.Type.INVITE) {
            showIncomingInvite(event.getChannel());
        }
    }

    private void showIncomingInvite(final Channel channel)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (incomingChannelInvite != null && incomingChannelInvite.isShowing()) {
                    incomingChannelInvite.dismiss();
                }
                incomingChannelInvite = new MaterialDialog.Builder(BaseActivity.this)
                        .title(R.string.incoming_call)
                        .content(R.string.incoming_call_message)
                        .positiveText(R.string.join)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                channel.join(new Constants.StatusListener() {
                                    @Override
                                    public void onError(ErrorInfo errorInfo)
                                    {
                                        MainApplication.get().logErrorInfo(
                                                "Failed to join channel", errorInfo);
                                    }

                                    @Override
                                    public void onSuccess()
                                    {
                                        //TODO notify update list
                                        logger.d("Successfully joined channel");
                                    }
                                });
                            }
                        })
                        .negativeText(R.string.decline)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                channel.declineInvitation(new Constants.StatusListener() {

                                    @Override
                                    public void onError(ErrorInfo errorInfo)
                                    {
                                        MainApplication.get().logErrorInfo(
                                                "Failed to decline channel invite", errorInfo);
                                    }

                                    @Override
                                    public void onSuccess()
                                    {
                                        logger.d("Successfully declined channel invite");
                                    }

                                });
                            }
                        })
                        .show();
            }
        });
    }

    @Subscribe
    public void onIncomingInvite(final IncomingInvite incomingInvite) {
        logger.d("onIncomingInvite");
        Member member = new Member("", incomingInvite.getInviter(), 0l, null, 0);
        try {
            Channel[] channels = MainApplication.get().getBasicClient().getIpMessagingClient().getChannels().getChannels();
            for (Channel channel : channels) {
                if (channel.getStatus() == Channel.ChannelStatus.JOINED) {
                    for (Member m : channel.getMembers().getMembers()) {
                        if (m.getUserInfo().getIdentity().equalsIgnoreCase(incomingInvite.getInviter())) {
                            member = m;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        if (getCurrentConversation() == null) {
            callInviteDialog.show(member, new UserInfoDialog.UserInfoListener() {
                @Override
                public void clickCall(Member member) {
                    MainApplication.get().setIncomingInvite(incomingInvite);
                    Intent intent = new Intent(BaseActivity.this, ConversationActivity.class);
                    intent.putExtra(ConversationActivity.VIDEO_ACTION, ConversationActivity.ACTION_ACCEPT_CALL);
                    startActivity(intent);
                }

                @Override
                public void clickCancelCall(Member member) {
                    incomingInvite.reject();
                }

                @Override
                public void clickChat(Member member) {

                }
            }, true);
        } else {
            logger.d(String.format("Conversation in progress. Invite from %s ignored",
                    incomingInvite.getInviter()));
            incomingInvite.reject();
        }
    }
}
