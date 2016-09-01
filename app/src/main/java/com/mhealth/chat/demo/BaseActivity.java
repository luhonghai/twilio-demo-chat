package com.mhealth.chat.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mhealth.chat.demo.data.UserPreference;
import com.mhealth.chat.demo.view.UserInfoDialog;
import com.twilio.common.AccessManager;
import com.twilio.conversations.AudioOutput;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.IncomingInvite;
import com.twilio.conversations.LogLevel;
import com.twilio.conversations.TwilioConversationsClient;
import com.twilio.conversations.TwilioConversationsException;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Member;

/**
 * Created by luhonghai on 9/1/16.
 */

public class BaseActivity extends AppCompatActivity implements TwilioConversationsClient.Listener, AccessManager.Listener {

    protected Logger logger = Logger.getLogger(this.getClass());

    protected TwilioConversationsClient conversationsClient;

    protected AccessManager accessManager;

    private UserInfoDialog callInviteDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        callInviteDialog = new UserInfoDialog(this);
        initializeTwilioSdk();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.get().setInApplication(true);
        if (TwilioConversationsClient.isInitialized() &&
                conversationsClient != null &&
                !conversationsClient.isListening()) {
            conversationsClient.listen();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.get().setInApplication(false);
        if (TwilioConversationsClient.isInitialized() &&
                conversationsClient != null  &&
                conversationsClient.isListening() &&
                getCurrentConversation() == null) {
            conversationsClient.unlisten();
        }
    }


    protected Conversation getCurrentConversation() {
        return null;
    }

    /*
             * Initialize the Twilio Conversations SDK
             */
    private void initializeTwilioSdk(){
        TwilioConversationsClient.setLogLevel(LogLevel.ERROR);
        if(!TwilioConversationsClient.isInitialized()) {
            TwilioConversationsClient.initialize(this.getApplicationContext());
        }
        accessManager = new AccessManager(this,
                new UserPreference(this).getVideoAccessToken(),
                this);
        conversationsClient =
                TwilioConversationsClient
                        .create(accessManager,
                                this);
        // Specify the audio output to use for this conversation client
        conversationsClient.setAudioOutput(AudioOutput.SPEAKERPHONE);
        conversationsClient.listen();
    }

    @Override
    protected void onDestroy() {
        TwilioConversationsClient.destroy();
        if (callInviteDialog != null) callInviteDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public void onTokenExpired(AccessManager twilioAccessManager) {
        logger.d("onAccessManagerTokenExpire");

    }

    @Override
    public void onTokenUpdated(AccessManager twilioAccessManager) {
        logger.d("onTokenUpdated");

    }

    @Override
    public void onError(AccessManager twilioAccessManager, String s) {
        logger.d("onError");
    }

    /*
     * ConversationsClient listener
     */
    @Override
    public void onStartListeningForInvites(TwilioConversationsClient conversationsClient) {
        logger.d("onStartListeningForInvites");
    }

    @Override
    public void onStopListeningForInvites(TwilioConversationsClient conversationsClient) {
        logger.d("onStopListeningForInvites");
        // If we are logging out let us finish the teardown process

    }

    @Override
    public void onFailedToStartListening(TwilioConversationsClient conversationsClient,
                                         TwilioConversationsException e) {
        logger.d("onFailedToStartListening");
    }

    @Override
    public void onIncomingInvite(TwilioConversationsClient conversationsClient,
                                 final IncomingInvite incomingInvite) {
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

    @Override
    public void onIncomingInviteCancelled(TwilioConversationsClient conversationsClient,
                                          IncomingInvite incomingInvite) {
        logger.d("onIncomingInviteCancelled");
        logger.d("Invite from " +
                incomingInvite.getInviter() + " terminated");
    }
}
