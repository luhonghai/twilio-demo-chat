package com.mhealth.chat.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.data.ChatConsultSession;
import com.mhealth.chat.demo.event.ChannelEvent;
import com.mhealth.chat.demo.fcm.ChatConsultRequestData;
import com.mhealth.chat.demo.fcm.NotificationObject;
import com.mhealth.chat.demo.view.UserInfoDialog;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationCallback;
import com.twilio.conversations.IncomingInvite;
import com.twilio.conversations.LocalMedia;
import com.twilio.conversations.Participant;
import com.twilio.conversations.TwilioConversationsException;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Channels;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.Member;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by luhonghai on 9/1/16.
 */

public class BaseActivity extends AppCompatActivity {

    private static final long MAX_REQUEST_TIMEOUT = 15000;

    protected Logger logger = Logger.getLogger(this.getClass());

    private UserInfoDialog callInviteDialog;

    private MaterialDialog incomingChannelInvite;

    private Handler handlerTimeout = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        callInviteDialog = new UserInfoDialog(this);
        checkIntent(getIntent());
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkIntent(intent);
    }

    private void checkIntent(Intent intent) {
        NotificationObject notificationObject = NotificationObject.from(intent);
        if (notificationObject != null) {
            logger.d("Found notification object from intent");
            onNotificationObjectFound(notificationObject);
        }
    }

    @Subscribe
    public void onNotificationEvent(NotificationObject object) {
        onNotificationObjectFound(object);
    }

    public void onNotificationObjectFound(NotificationObject object) {
        logger.d("onNotificationObjectFound " + object.getType().getName());
        if (object.getType() == NotificationObject.Type.CHAT_CONSULT_REQUEST) {
            ChatConsultRequestData data = (ChatConsultRequestData) object.getData();
            logger.d("Request session ID " + data.getSessionId());
        }
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
        handlerTimeout.removeCallbacksAndMessages(null);
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
        IPMessagingClient messagingClient = MainApplication.get().getBasicClient().getIpMessagingClient();
        final String currentUser = messagingClient.getMyUserInfo().getIdentity();
        messagingClient.getMyUserInfo();
        Channels channelObject = messagingClient.getChannels();
        Channel generalChannel = channelObject.getChannelByUniqueName("general");

        Set<String> participants = incomingInvite.getParticipants();
        try {
            Field f = incomingInvite.getClass().getDeclaredField("conversation");
            f.setAccessible(true);
            Conversation conversation = (Conversation) f.get(incomingInvite);
            Set<Participant> participantSet = conversation.getParticipants();
            if (participantSet.size() > 0) {
                participants = new HashSet<>();
                for (Participant participant : participantSet) {
                    logger.d("Found participants from conversation " + participant.getIdentity());
                    participants.add(participant.getIdentity());
                }
                if (generalChannel != null) {
                    Member[] members = generalChannel.getMembers().getMembers();
                    for (Member member : members) {
                        if (member.getUserInfo().getIdentity().equalsIgnoreCase(incomingInvite.getInviter())) {
                            logger.d("Found member " + incomingInvite.getInviter());
                            try {
                                String sessionId = member.getUserInfo().getAttributes().optString("chat_consult_session");
                                logger.d("Found chat session id " + sessionId);
                                participants.add(sessionId);
                            } catch (Exception e) {e.printStackTrace();}
                            break;
                        }
                    }
                } else {
                    logger.e("General channel not found");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String channelName = "";
        if (participants!= null && participants.size() > 0) {
            logger.d("Found participants size " + participants.size());
            for (String participant : participants) {
                logger.d("Found participant " + participant);
                if (participant.toLowerCase().startsWith(Constant.CHAT_CONSULT_PREFIX)) {
                    channelName = participant;
                    break;
                }
            }
        }
        if (channelName.length() > 0) {
            showChatConsultInvite(channelName, incomingInvite);
        } else {
            Member member = new Member("", incomingInvite.getInviter(), 0l, null, 0);
            try {
                Channel[] channels = channelObject.getChannels();
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
                handlerTimeout.removeCallbacksAndMessages(null);
                handlerTimeout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        incomingInvite.reject();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callInviteDialog.dismiss();
                            }
                        });
                    }
                }, MAX_REQUEST_TIMEOUT);
                callInviteDialog.show(member, new UserInfoDialog.UserInfoListener() {
                    @Override
                    public void clickCall(Member member) {
                        handlerTimeout.removeCallbacksAndMessages(null);
                        callInviteDialog.dismiss();
                        MainApplication.get().setIncomingInvite(incomingInvite);
                        Intent intent = new Intent(BaseActivity.this, ConversationActivity.class);
                        intent.putExtra(ConversationActivity.VIDEO_ACTION, ConversationActivity.ACTION_ACCEPT_CALL);
                        startActivity(intent);
                    }

                    @Override
                    public void clickCancelCall(Member member) {
                        handlerTimeout.removeCallbacksAndMessages(null);
                        callInviteDialog.dismiss();
                        incomingInvite.reject();
                    }

                    @Override
                    public void clickChat(Member member) {

                    }
                }, UserInfoDialog.Type.INVITE);
            } else {
                logger.d(String.format("Conversation in progress. Invite from %s ignored",
                        incomingInvite.getInviter()));
                incomingInvite.reject();
            }
        }
    }

    private void prepareForChatConsult(final String channelSession,
                                       final IncomingInvite incomingInvite) {
        IPMessagingClient messagingClient = MainApplication.get().getBasicClient().getIpMessagingClient();
        Channels channelObject = messagingClient.getChannels();
        ChatConsultSession session = ChatConsultSession.decodeChannelName(channelSession);
        final String channelName;
        if (session != null) {
            channelName = ChatConsultSession.CHAT_CONSULT_PREFIX + session.getSessionId();
        } else {
            channelName = channelSession;
        }
        logger.d("Chat consult request from member " + incomingInvite.getInviter()
                + " channel unique name " + channelName);
        final Channel channel = channelObject.getChannelByUniqueName(channelName);
        if (channel != null) {
            if (channel.getStatus() == Channel.ChannelStatus.JOINED) {
                acceptChatConsult(channel, incomingInvite);
            } else {
                channel.join(new Constants.StatusListener() {
                    @Override
                    public void onSuccess() {
                        acceptChatConsult(channel, incomingInvite);
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        MainApplication.get().showError(errorInfo);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callInviteDialog.dismiss();
                            }
                        });
                    }
                });
            }
        } else {
            logger.e("Could not found this channel. Create new channel " + channelName);
            Map<String, Object> data = new HashMap<>();
            data.put("friendlyName", "Chat Consult");
            data.put("uniqueName", channelName);
            data.put("ChannelType", Channel.ChannelType.PRIVATE);
            channelObject.createChannel(data, new Constants.CreateChannelListener() {
                @Override
                public void onCreated(final Channel channel) {
                    channel.join(new Constants.StatusListener() {
                        @Override
                        public void onSuccess() {
                            acceptChatConsult(channel, incomingInvite);
                        }

                        @Override
                        public void onError(ErrorInfo errorInfo) {
                            MainApplication.get().showError(errorInfo);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callInviteDialog.dismiss();
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    logger.e("could not create channel " + channelName + " error " + errorInfo.getErrorText());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callInviteDialog.dismiss();
                        }
                    });
                }
            });
        }
    }

    private void acceptChatConsult(final Channel channel, final IncomingInvite incomingInvite) {
        channel.getMembers().inviteByIdentity(incomingInvite.getInviter(), new Constants.StatusListener() {
            @Override
            public void onSuccess() {
                incomingInvite.accept(new LocalMedia(null), new ConversationCallback() {
                    @Override
                    public void onConversation(Conversation conversation, TwilioConversationsException e) {
                        conversation.disconnect();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callInviteDialog.dismiss();
                                Intent intent = new Intent(BaseActivity.this, MessageActivity.class);
                                intent.putExtra("C_SID", channel.getSid());
                                startActivity(intent);
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                MainApplication.get().showError(errorInfo);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callInviteDialog.dismiss();
                    }
                });
            }
        });

    }

    private void showChatConsultInvite(final String channelName,
                                       final IncomingInvite incomingInvite) {
        handlerTimeout.removeCallbacksAndMessages(null);
        handlerTimeout.postDelayed(new Runnable() {
            @Override
            public void run() {
                incomingInvite.reject();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callInviteDialog.dismiss();
                    }
                });
            }
        }, MAX_REQUEST_TIMEOUT);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Member requestMember = new Member(channelName, "", 0l, null, 0);
                callInviteDialog.show(requestMember, new UserInfoDialog.UserInfoListener() {
                    @Override
                    public void clickCall(Member member) {
                        handlerTimeout.removeCallbacksAndMessages(null);
                        callInviteDialog.showProgress();
                        prepareForChatConsult(channelName, incomingInvite);
                    }

                    @Override
                    public void clickCancelCall(Member member) {
                        handlerTimeout.removeCallbacksAndMessages(null);
                        callInviteDialog.dismiss();
                        incomingInvite.reject();
                    }

                    @Override
                    public void clickChat(Member member) {

                    }
                }, UserInfoDialog.Type.CHAT_CONSULT);
            }
        });
    }
}
