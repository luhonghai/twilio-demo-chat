package com.mhealth.chat.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.ChannelListener;
import com.twilio.ipmessaging.Channels;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import uk.co.ribot.easyadapter.EasyAdapter;

/**
 * Created by luhonghai on 8/30/16.
 */

public class ChannelFragment extends Fragment implements ChannelListener {

    private static final Logger logger = Logger.getLogger(ChannelFragment.class);

    private static final String[] CHANNEL_OPTIONS = { "Join" };

    private static final int JOIN = 0;

    private static final String CHANNEL_TYPE = "CHANNEL_TYPE";

    private GridView listView;
    private BasicIPMessagingClient basicClient;
    private List<Channel> channels = new ArrayList<Channel>();
    private EasyAdapter<Channel> adapter;
    private Channels channelsObject;
    private Channel[] channelArray;

    private static final Handler handler = new Handler();
    private AlertDialog          incomingChannelInvite;
    private Constants.StatusListener joinListener;
    private Constants.StatusListener declineInvitationListener;

    public static ChannelFragment getInstance(Channel.ChannelType type) {
        ChannelFragment fragment = new ChannelFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(CHANNEL_TYPE, type.getValue());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel, container, false);
        basicClient = MainApplication.get().getBasicClient();
        if (basicClient != null && basicClient.getIpMessagingClient() != null) {
            setupListView(root);
        }
        EventBus.getDefault().register(this);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onActionEvent(ActionEvent event) {
        switch (event.getAction()) {
            case GROUP_ADDED:
            case CHANNELS_UPDATED:
                getChannels();
                break;
        }
    }

    public Channel.ChannelType getChannelType() {
        Bundle args = getArguments();
        return (args != null &&
                args.containsKey(CHANNEL_TYPE))
                ? Channel.ChannelType.fromInt(args.getInt(CHANNEL_TYPE))
                    : Channel.ChannelType.PUBLIC;
    }



    @Override
    public void onResume()
    {
        super.onResume();
        if (getActivity() != null) {
            handleIncomingIntent(getActivity().getIntent());
            getChannels();
        }
    }

    private boolean handleIncomingIntent(Intent intent)
    {
        if (intent != null) {
            Channel channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
            String  action = intent.getStringExtra(Constants.EXTRA_ACTION);
            intent.removeExtra(Constants.EXTRA_CHANNEL);
            intent.removeExtra(Constants.EXTRA_ACTION);
            if (action != null) {
                if (action.compareTo(Constants.EXTRA_ACTION_INVITE) == 0) {
                    this.showIncomingInvite(channel);
                }
            }
        }
        return false;
    }

    private void setupListView(View root)
    {
        listView = (GridView) root.findViewById(R.id.channel_list);
        listView.setEmptyView(root.findViewById(R.id.no_result));
        adapter = new EasyAdapter<Channel>(
                getContext(),
                ChannelViewHolder.class,
                channels,
                new ChannelViewHolder.OnChannelClickListener() {
                    @Override
                    public void onChannelClicked(final Channel channel)
                    {
                        if (channel.getStatus() == Channel.ChannelStatus.JOINED) {
                            final Channel channelSelected = channelsObject.getChannel(channel.getSid());
                            if (channelSelected != null) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        Intent i =
                                                new Intent(getActivity(), MessageActivity.class);
                                        i.putExtra(Constants.EXTRA_CHANNEL,
                                                (Parcelable)channelSelected);
                                        i.putExtra("C_SID", channelSelected.getSid());
                                        startActivity(i);
                                    }
                                }, 0);
                            }
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Join this group?")
                                .setItems(CHANNEL_OPTIONS, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        if (which == JOIN) {
                                            dialog.cancel();
                                            joinListener = new Constants.StatusListener() {
                                                @Override
                                                public void onError(ErrorInfo errorInfo)
                                                {
                                                    MainApplication.get().logErrorInfo(
                                                            "failed to join channel", errorInfo);
                                                }

                                                @Override
                                                public void onSuccess()
                                                {
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                adapter.notifyDataSetChanged();
                                                            }
                                                        });
                                                    }
                                                    logger.d("Successfully joined channel");

                                                    Intent i =
                                                            new Intent(getActivity(), MessageActivity.class);
                                                    i.putExtra(Constants.EXTRA_CHANNEL,
                                                            (Parcelable)channel);
                                                    i.putExtra("C_SID", channel.getSid());
                                                    startActivity(i);
                                                }
                                            };
                                            channel.join(joinListener);
                                        }
                                    }
                                });
                        builder.show();
                    }
                });
        listView.setAdapter(adapter);
    }

    private void getChannels()
    {
        if (basicClient != null && basicClient.getIpMessagingClient() != null) {
            channelsObject = basicClient.getIpMessagingClient().getChannels();
            channels.clear();
            if (channelsObject != null) {
                channelArray = channelsObject.getChannels();
                List<Channel> channelList = new ArrayList<>();
                if (channelArray != null && channelArray.length > 0) {
                    for (final Channel channel : channelArray) {
                        if (channel.getType() == getChannelType()) {
                            channelList.add(channel);
                        }
                    }
                    Collections.sort(channelList, new CustomChannelComparator());
                }
                setupListenersForChannel(channelList);
                if (channelList.size() > 0) {
                    channels.addAll(channelList);
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void showIncomingInvite(final Channel channel)
    {
        handler.post(new Runnable() {
            @Override
            public void run()
            {
                if (incomingChannelInvite == null) {

                    incomingChannelInvite =
                            new AlertDialog.Builder(getContext())
                                    .setTitle(R.string.incoming_call)
                                    .setMessage(R.string.incoming_call_message)
                                    .setPositiveButton(
                                            R.string.join,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
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
                                                            if (getActivity() != null) {
                                                                getActivity().runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        adapter.notifyDataSetChanged();
                                                                    }
                                                                });
                                                                logger.d("Successfully joined channel");

                                                            }
                                                        }
                                                    });
                                                    incomingChannelInvite = null;
                                                }
                                            })
                                    .setNegativeButton(
                                            R.string.decline,
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    declineInvitationListener = new Constants.StatusListener() {

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

                                                    };
                                                    channel.declineInvitation(declineInvitationListener);
                                                    incomingChannelInvite = null;
                                                }
                                            })
                                    .create();
                    incomingChannelInvite.show();
                }
            }
        });
    }

    private class CustomChannelComparator implements Comparator<Channel>
    {
        @Override
        public int compare(Channel lhs, Channel rhs)
        {
            return lhs.getFriendlyName().compareTo(rhs.getFriendlyName());
        }
    }

    private void setupListenersForChannel(final List<Channel> channelArray)
    {
        if (channelArray != null) {
            for(final Channel channel : channelArray) {
                channel.setListener(this);
            }
        }
    }

    private void showToast(String text)
    {
        Toast toast = Toast.makeText(getContext().getApplicationContext(), text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    // ChannelListener implementation

    @Override
    public void onTypingStarted(Member member)
    {
        if (member != null) {
            logger.d(member.getUserInfo().getIdentity() + " started typing");
        }
    }

    @Override
    public void onTypingEnded(Member member)
    {
        if (member != null) {
            logger.d(member.getUserInfo().getIdentity() + " ended typing");
        }
    }

    @Override
    public void onSynchronizationChange(Channel channel)
    {
        logger.e("Received onSynchronizationChange callback for channel |"
                + channel.getFriendlyName()
                + "|");
    }

    // Message-related callbacks

    @Override
    public void onMessageAdd(Message message)
    {
        if (message != null) {
            logger.d("Received onMessageAdd event");
        }
    }

    @Override
    public void onMessageChange(Message message)
    {
        if (message != null) {
            logger.d("Received onMessageChange event");
        }
    }

    @Override
    public void onMessageDelete(Message message)
    {
        logger.d("Received onMessageDelete event");
    }

    // Member-related callbacks

    @Override
    public void onMemberJoin(Member member)
    {
        if (member != null) {
            logger.d("Member " + member.getUserInfo().getIdentity() + " joined");
        }
    }

    @Override
    public void onMemberChange(Member member)
    {
        if (member != null) {
            logger.d("Member " + member.getUserInfo().getIdentity() + " changed");
        }
    }

    @Override
    public void onMemberDelete(Member member)
    {
        if (member != null) {
            logger.d("Member " + member.getUserInfo().getIdentity() + " deleted");
        }
    }

    @Override
    public void onAttributesChange(Map<String, String> map)
    {
    }
}
