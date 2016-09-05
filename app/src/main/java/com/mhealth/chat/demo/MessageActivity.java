package com.mhealth.chat.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.event.ChannelEvent;
import com.mhealth.chat.demo.view.UserInfoDialog;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.ChannelListener;
import com.twilio.ipmessaging.Channels;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.ErrorInfo;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Members;
import com.twilio.ipmessaging.Message;
import com.twilio.ipmessaging.Messages;
import com.twilio.ipmessaging.internal.Logger;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.ribot.easyadapter.EasyAdapter;

public class MessageActivity extends BaseActivity implements ChannelListener, MessageViewHolder.MessageItemAdapter
{

    private static final int MESSAGE_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 30;
    private static final Logger logger = Logger.getLogger(MessageActivity.class);
    private static final        String[] MESSAGE_OPTIONS = {
        "Remove", "Edit"
    };
    private ListView                 messageListView;
    private EditText                 inputText;
    private EasyAdapter<MessageItem> adapter;
    private List<Member>             members = new ArrayList<Member>();
    private Channel                  channel;
    private static final             String[] EDIT_OPTIONS = { "Change Friendly Name",
                                                   "Change Topic",
                                                   "List Members",
                                                   "Invite Member",
                                                   "Add Member",
                                                   "Remove Member",
                                                   "Leave",
                                                   "Change ChannelType",
                                                   "Destroy",
                                                   "Get Attributes",
                                                   "Change Unique Name",
                                                   "Get Unique Name" };

    private static final int NAME_CHANGE = 0;
    private static final int TOPIC_CHANGE = 1;
    private static final int LIST_MEMBERS = 2;
    private static final int INVITE_MEMBER = 3;
    private static final int ADD_MEMBER = 4;
    private static final int REMOVE_MEMBER = 5;
    private static final int LEAVE = 6;
    private static final int CHANNEL_TYPE = 7;
    private static final int CHANNEL_DESTROY = 8;
    private static final int CHANNEL_ATTRIBUTE = 9;
    private static final int SET_CHANNEL_UNIQUE_NAME = 10;
    private static final int GET_CHANNEL_UNIQUE_NAME = 11;

    private static final int REMOVE = 0;
    private static final int EDIT = 1;
    private static final int GET_ATTRIBUTES = 2;
    private static final int SET_ATTRIBUTES = 3;
    private static final int CALL = 4;

    private AlertDialog            editTextDialog;
    private AlertDialog            memberListDialog;
    private AlertDialog            changeChannelTypeDialog;
    private StatusListener         messageListener;
    private StatusListener         leaveListener;
    private StatusListener         destroyListener;
    private StatusListener         nameUpdateListener;
    private ArrayList<MessageItem> messageItemList;
    private String                 identity;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.progress_bar)
    View progressBar;

    @Bind(R.id.progress_bar_bottom)
    View progressBarBottom;

    @Bind(R.id.sendButton)
    View btnSend;

    private UserInfoDialog userInfoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        userInfoDialog = new UserInfoDialog(this);
        createUI();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userInfoDialog != null) {
            userInfoDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.get().setCurrentChannelSid("");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        MainApplication.get().setCurrentChannelSid(channel.getSid());
        Intent intent = getIntent();
        if (intent != null) {
            Channel channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
            if (channel != null) {
                setupListView(channel);
            }
        }
    }

    private void createUI()
    {
        if (MainApplication.get().getBasicClient().getIpMessagingClient() == null) {
            this.finish();
            return;
        }
        setContentView(R.layout.activity_message);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("");
        if (getIntent() != null) {
            TwilioClient basicClient = MainApplication.get().getBasicClient();
            identity = basicClient.getIpMessagingClient().getMyUserInfo().getIdentity();
            String   channelSid = getIntent().getStringExtra("C_SID");
            Channels channelsObject = basicClient.getIpMessagingClient().getChannels();
            if (channelsObject != null) {
                channel = channelsObject.getChannel(channelSid);
                if (channel != null) {
                    channel.setListener(MessageActivity.this);
                    updateTitle();
                }
            }
        }

        channel.synchronize(new Constants.CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo)
            {
                MainApplication.get().logErrorInfo("Channel sync failed", errorInfo);
            }

            @Override
            public void onSuccess(Channel result)
            {
                logger.d("Channel sync success for " + result.getFriendlyName());
            }
        });

        setupListView(channel);
        messageListView = (ListView)findViewById(R.id.message_list_view);
        setupInput();
    }

    private void updateTitle() {
        TwilioChannel twilioChannel = MainApplication.get().getChannelDataPreference().get(channel.getSid());
        if (twilioChannel != null) {
            getSupportActionBar().setTitle(twilioChannel.getFriendlyName());
        } else {
            getSupportActionBar().setTitle(channel.getFriendlyName());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_info:
                Intent intent = new Intent(this, ChannelDetailActivity.class);
                intent.putExtra(Channel.class.getName(), channel);
                startActivityForResult(intent, ActivityResultCommon.ACTION_EDIT_GROUP);
                break;
            case R.id.action_settings: showChannelSettingsDialog(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChannelSettingsDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setTitle("Select an option")
            .setItems(EDIT_OPTIONS, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which)
                {
                    if (which == NAME_CHANGE) {
                        showChangeNameDialog();
                    } else if (which == TOPIC_CHANGE) {
                        showChangeTopicDialog();
                    } else if (which == LIST_MEMBERS) {
                        Members membersObject = channel.getMembers();
                        Member[] members = membersObject.getMembers();

                        logger.d("members retrieved");
                        StringBuffer name = new StringBuffer();
                        for (int i = 0; i < members.length; i++) {
                            name.append(members[i].getUserInfo().getIdentity());
                            if (i + 1 < members.length) {
                                name.append(", ");
                            }
                        }
                        showToast(name.toString());
                    } else if (which == INVITE_MEMBER) {
                        showInviteMemberDialog();
                    } else if (which == ADD_MEMBER) {
                        showAddMemberDialog();
                    } else if (which == LEAVE) {
                        leaveListener = new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().logErrorInfo("Error leaving channel",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at leaving channel");
                                finish();
                            }
                        };
                        channel.leave(leaveListener);

                    } else if (which == REMOVE_MEMBER) {
                        showRemoveMemberDialog();
                    } else if (which == CHANNEL_TYPE) {
                        showChangeChannelType();
                    } else if (which == CHANNEL_DESTROY) {
                        destroyListener = new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().logErrorInfo("Error destroying channel",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at destroying channel");
                                finish();
                            }
                        };
                        channel.destroy(destroyListener);
                    } else if (which == CHANNEL_ATTRIBUTE) {
                        showToast(channel.getAttributes().toString());
                    } else if (which == SET_CHANNEL_UNIQUE_NAME) {
                        showChangeUniqueNameDialog();
                    } else if (which == GET_CHANNEL_UNIQUE_NAME) {
                        showToast(channel.getUniqueName());
                    }
                }
            });

        builder.show();
    }

    private void showChangeNameDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_friendly_name, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String friendlyName =
                            ((EditText)editTextDialog.findViewById(R.id.update_friendly_name))
                                .getText()
                                .toString();
                        logger.d(friendlyName);
                        nameUpdateListener = new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().showError(errorInfo);
                                MainApplication.get().logErrorInfo("Error changing name",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("successfully changed name");
                            }
                        };
                        channel.setFriendlyName(friendlyName, nameUpdateListener);
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showChangeTopicDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_channel_topic, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String topic = ((EditText)editTextDialog.findViewById(R.id.update_topic))
                                           .getText()
                                           .toString();
                        logger.d(topic);
                        JSONObject attrObj = new JSONObject();
                        try {
                            attrObj.put("Topic", topic);
                        } catch (JSONException ignored) {
                            // whatever
                        }

                        channel.setAttributes(attrObj, new StatusListener() {
                            @Override
                            public void onSuccess()
                            {
                                logger.d("Attributes were set successfully.");
                            }

                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().showError(errorInfo);
                                MainApplication.get().logErrorInfo("Setting attributes failed",
                                                                     errorInfo);
                            }
                        });
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showInviteMemberDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_invite_member, null))
            .setPositiveButton(
                "Invite",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String memberName =
                            ((EditText)editTextDialog.findViewById(R.id.invite_member))
                                .getText()
                                .toString();
                        logger.d(memberName);

                        Members membersObject = channel.getMembers();
                        membersObject.inviteByIdentity(memberName, new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().showError(errorInfo);
                                MainApplication.get().logErrorInfo("Error in inviteByIdentity",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at inviteByIdentity.");
                            }
                        });
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showAddMemberDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_add_member, null))
            .setPositiveButton(
                "Add",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String memberName = ((EditText)editTextDialog.findViewById(R.id.add_member))
                                                .getText()
                                                .toString();
                        logger.d(memberName);

                        Members membersObject = channel.getMembers();
                        membersObject.addByIdentity(memberName, new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().showError(errorInfo);
                                MainApplication.get().logErrorInfo("Error adding member",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at addByIdentity");
                            }
                        });
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showRemoveMemberDialog()
    {
        final Members membersObject = channel.getMembers();
        Member[] membersArray = membersObject.getMembers();
        if (membersArray.length > 0) {
            members = new ArrayList<Member>(Arrays.asList(membersArray));
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MessageActivity.this);
        View convertView = (View)getLayoutInflater().inflate(R.layout.member_list, null);
        alertDialog.setView(convertView);
        alertDialog.setTitle("Remove members");
        ListView            lv = (ListView)convertView.findViewById(R.id.listView1);
        EasyAdapter<Member> adapterMember = new EasyAdapter<Member>(
            this, MemberViewHolder.class, members, new MemberViewHolder.OnMemberClickListener() {
                @Override
                public void onMemberClicked(Member member)
                {
                    membersObject.removeMember(member, new StatusListener() {
                        @Override
                        public void onError(ErrorInfo errorInfo)
                        {
                            MainApplication.get().showError(errorInfo);
                            MainApplication.get().logErrorInfo("Error in removeMember operation",
                                                                 errorInfo);
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("Successful removeMember operation");
                        }
                    });
                    memberListDialog.dismiss();
                }
            });
        lv.setAdapter(adapterMember);
        memberListDialog = alertDialog.create();
        memberListDialog.show();
        memberListDialog.getWindow().setLayout(800, 600);
    }

    private void showChangeChannelType()
    {
    }

    private void showUpdateMessageDialog(final Message message)
    {
        new MaterialDialog.Builder(this)
                .title("Update message")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("Enter new message", message.getMessageBody(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, final CharSequence input) {
                        final String updatedMsg = input.toString();
                        if (updatedMsg.isEmpty()) return;
                        message.updateMessageBody(updatedMsg, new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().showError(errorInfo);
                                MainApplication.get().logErrorInfo("Error updating message",
                                        errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Success updating message");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        setupListView(channel);
                                    }
                                });
                            }
                        });
                    }
                })
                .positiveText("Update")
                .negativeText("Cancel")
                .show();
    }

    private void showUpdateMessageAttributesDialog(final Message message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_message_attributes, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String updatedAttr =
                            ((EditText)editTextDialog.findViewById(R.id.update_attributes))
                                .getText()
                                .toString();
                        JSONObject jsonObj = null;
                        try {
                            jsonObj = new JSONObject(updatedAttr);
                        } catch (JSONException e) {
                            logger.e("Invalid JSON attributes entered, using old value");
                            jsonObj = message.getAttributes();
                        }

                        message.setAttributes(jsonObj, new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                MainApplication.get().showError(errorInfo);
                                MainApplication.get().logErrorInfo(
                                    "Error updating message attributes", errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Success updating message attributes");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        setupListView(channel);
                                    }
                                });
                            }
                        });
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();

        editTextDialog.create(); // Force creation of sub-view hierarchy
        ((EditText)editTextDialog.findViewById(R.id.update_attributes))
            .setText(message.getAttributes().toString());

        editTextDialog.show();
    }

    private void setupInput()
    {
        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText)findViewById(R.id.messageInput);
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }
            @Override
            public void afterTextChanged(Editable s)
            {
                if (channel != null) {
                    channel.typing();
                }
            }
        });

        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
            {
                if (actionId == EditorInfo.IME_NULL
                    && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });
    }

    @OnClick(R.id.sendButton)
    public void clickButtonSend() {
        sendMessage();
    }

    private class CustomMessageComparator implements Comparator<Message>
    {
        @Override
        public int compare(Message lhs, Message rhs)
        {
            if (lhs == null) {
                return (rhs == null) ? 0 : -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
        }
    }

    private long lastMessageIndex = -1;

    private void setupListView(final Channel channel)
    {
        messageListView = (ListView)findViewById(R.id.message_list_view);
        final Messages messagesObject = channel.getMessages();
        if (messagesObject != null) {
            messagesObject.getLastMessages(MESSAGE_PAGE_SIZE, new Constants.CallbackListener<List<Message>>() {
                @Override
                public void onSuccess(final List<Message> messages) {
                    logger.d("Found " + messages.size() + " messages");
                    if (messages.size() == 0) return;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setEnableScrollToBottom(true);
                            Members members = channel.getMembers();
                            Collections.sort(messages, new CustomMessageComparator());
                            MessageItem[] items = new MessageItem[messages.size()];
                            for (int i = 0; i < items.length; i++) {
                                items[i] = new MessageItem(messages.get(i), members, identity);
                            }
                            messageItemList = new ArrayList(Arrays.asList(items));
                            adapter = new EasyAdapter<MessageItem>(
                                    MessageActivity.this,
                                    MessageViewHolder.class,
                                    messageItemList,
                                    onMessageClickListener
                            );
                            messageListView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                            messageListView.getViewTreeObserver().addOnScrollChangedListener(onScrollChangedListener);
//                            if (lastMessageIndex == -1) {
//                                messageListView.setSelection(adapter.getCount() - 1);
//                            } else {
//                                messageListView.smoothScrollToPosition(adapter.getCount() - 1);
//                            }
                            lastMessageIndex = messages.get(0).getMessageIndex();
                        }
                    });
                }
            });
        }
    }

    private void setEnableScrollToBottom(boolean enable) {
        if (messageListView != null) {
            messageListView.setTranscriptMode(enable ? AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL :
                    AbsListView.TRANSCRIPT_MODE_DISABLED);
            messageListView.setStackFromBottom(enable);
        }
    }

    private ViewTreeObserver.OnScrollChangedListener onScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged()
        {
            if (adapter == null || channel == null) return;
            Messages messagesObject = channel.getMessages();
            if ((messageListView.getLastVisiblePosition() >= 0)
                    && (messageListView.getLastVisiblePosition() < adapter.getCount())) {
                MessageItem item =
                        adapter.getItem(messageListView.getLastVisiblePosition());

                if (item != null && messagesObject != null) {
                    messagesObject.advanceLastConsumedMessageIndex(
                            item.getMessage().getMessageIndex());
                    logger.d("getLastVisiblePosition " + messageListView.getLastVisiblePosition() + " Message index:"
                            + item.getMessage().getMessageIndex()
                            +". Text: " + item.getMessage().getMessageBody()
                            + ". Author: " + item.getMessage().getAuthor());
                }
            }
            int pos = messageListView.getFirstVisiblePosition();
            if (pos >= 0
                    && pos < adapter.getCount()) {
                MessageItem item = adapter.getItem(pos);
                if (item != null && messagesObject != null) {
                    logger.d("getFirstVisiblePosition " + pos + " Message index:"
                            + item.getMessage().getMessageIndex() + ". Text: " + item.getMessage().getMessageBody()
                            + ". Author: " + item.getMessage().getAuthor()
                    + ". Current size " + messageItemList.size());
                    final long lIndex = Long.valueOf(lastMessageIndex);
                    if (lastMessageIndex != -1
                            && item.getMessage().getMessageIndex() == lastMessageIndex
                            && messageItemList != null && messageItemList.size() > 0
                            && messageItemList.size() % MESSAGE_PAGE_SIZE == 0
                            && messageItemList.size() <= MAX_PAGE_SIZE * MESSAGE_PAGE_SIZE) {
                        progressBar.setVisibility(View.VISIBLE);

                        messagesObject.getMessagesBefore(lastMessageIndex, MESSAGE_PAGE_SIZE, new Constants.CallbackListener<List<Message>>() {
                            @Override
                            public void onSuccess(final List<Message> mesMessages) {
                                logger.d("Found " + mesMessages.size() + " messages from index " + lIndex);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setEnableScrollToBottom(false);
                                        if (mesMessages.size() > 0) {
                                            Members members = channel.getMembers();
                                            Collections.sort(mesMessages, new CustomMessageComparator());
                                            lastMessageIndex = mesMessages.get(0).getMessageIndex();
                                            for (int i = mesMessages.size() - 1; i >= 0; i--) {
                                                MessageItem messageItem = new MessageItem(mesMessages.get(i), members, identity);
                                                messageItemList.add(0, messageItem);
                                            }
                                            adapter.notifyDataSetChanged();
                                            messageListView.clearFocus();
                                            messageListView.setFocusable(true);
                                            messageListView.setSelection(mesMessages.size());
                                        } else {
                                            lastMessageIndex = -1;
                                        }
                                        setEnableScrollToBottom(true);
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
                            }

                            @Override
                            public void onError(final ErrorInfo errorInfo) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.GONE);
                                        MainApplication.get().showError(errorInfo);
                                    }
                                });
                            }
                        });
                        lastMessageIndex = -1;
                    }
                }
            }
        }
    };

    private MessageViewHolder.OnMessageClickListener onMessageClickListener = new MessageViewHolder.OnMessageClickListener() {
        @Override
        public void onMessageClicked(final MessageItem message)
        {
            if (message.getCurrentUser().equalsIgnoreCase(message.getMessage().getAuthor())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle("Select an option")
                        .setItems(MESSAGE_OPTIONS, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == REMOVE) {
                                    dialog.cancel();
                                    channel.getMessages().removeMessage(
                                            message.getMessage(), new StatusListener() {
                                                @Override
                                                public void onError(ErrorInfo errorInfo) {
                                                    MainApplication.get().showError(errorInfo);
                                                    MainApplication.get().logErrorInfo(
                                                            "Error removing message", errorInfo);
                                                }

                                                @Override
                                                public void onSuccess() {
                                                    logger.d(
                                                            "Successfully removed message. It should be GONE!!");
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            messageItemList.remove(message);
                                                            adapter.notifyDataSetChanged();
                                                        }
                                                    });
                                                }
                                            });
                                } else if (which == EDIT) {
                                    showUpdateMessageDialog(message.getMessage());
                                }
                            }
                        });
                builder.show();
            }
        }

        @Override
        public void onMemberSelect(Member member) {
            userInfoDialog.show(member, new UserInfoDialog.UserInfoListener() {
                @Override
                public void clickCall(Member member) {
                    Intent intent = new Intent(MessageActivity.this, ConversationActivity.class);
                    intent.putExtra(ConversationActivity.VIDEO_ACTION, ConversationActivity.ACTION_CALL);
                    intent.putExtra(ConversationActivity.TARGET_IDENTITY, member.getUserInfo().getIdentity());
                    startActivity(intent);
                }

                @Override
                public void clickCancelCall(Member member) {

                }

                @Override
                public void clickChat(Member member) {

                }
            });
        }
    };

    private void sendMessage()
    {
        inputText = (EditText)findViewById(R.id.messageInput);
        final String input = inputText.getText().toString();
        if (!input.equals("")) {
            final Messages messagesObject = this.channel.getMessages();
            inputText.setText("");
            progressBarBottom.setVisibility(View.VISIBLE);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    messagesObject.sendMessage(input, new StatusListener() {
                        @Override
                        public void onError(ErrorInfo errorInfo)
                        {
                            MainApplication.get().showError(errorInfo);
                            MainApplication.get().logErrorInfo("Error sending message", errorInfo);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    inputText.requestFocus();
                                    progressBarBottom.setVisibility(View.GONE);
                                }
                            });
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("Successfully sent message.");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    inputText.requestFocus();
                                    progressBarBottom.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                    return null;
                }
            }.execute();

        }
        inputText.requestFocus();
    }

    public MessageItem getMessageItemByPosition(int pos) {
        if (adapter != null && pos >= 0 && pos < adapter.getCount()) {
            return adapter.getItem(pos);
        }
        return null;
    }

    @Subscribe
    public void onChannelUpdated(ChannelEvent channelEvent) {
        Channel mChannel = channelEvent.getChannel();
        if (mChannel != null && channel != null && channel.getSid().equalsIgnoreCase(mChannel.getSid())) {
            updateTitle();
        }
    }

    @Override
    public void onMessageAdd(Message message)
    {
        setupListView(this.channel);
    }

    @Override
    public void onMessageChange(Message message)
    {
        if (message != null) {
            //showToast(message.getSid() + " changed");
            setupListView(this.channel);
            logger.d("Received onMessageChange for message sid|" + message.getSid() + "|");
        } else {
            logger.d("Received onMessageChange");
        }
    }

    @Override
    public void onMessageDelete(Message message)
    {
        if (message != null) {
            //showToast(message.getSid() + " deleted");
            setupListView(this.channel);
            logger.d("Received onMessageDelete for message sid|" + message.getSid() + "|");
        } else {
            logger.d("Received onMessageDelete.");
        }
    }

    @Override
    public void onMemberJoin(Member member)
    {
        if (member != null) {
            //showToast(member.getUserInfo().getIdentity() + " joined");
        }
    }

    @Override
    public void onMemberChange(Member member)
    {
        if (member != null) {
            //showToast(member.getUserInfo().getIdentity() + " changed");
        }
    }

    @Override
    public void onMemberDelete(Member member)
    {
        if (member != null) {
            //showToast(member.getUserInfo().getIdentity() + " deleted");
        }
    }

    @Override
    public void onAttributesChange(Map<String, String> updatedAttributes)
    {
        logger.d("Deprecated: Received onAttributesChange event");
    }

    private void showToast(String text)
    {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    @Override
    public void onTypingStarted(Member member)
    {
        if (member != null) {
            TextView typingIndc = (TextView)findViewById(R.id.typingIndicator);
            String   text = member.getUserInfo().getIdentity() + " is typing ...";
            typingIndc.setText(text);
            logger.d(text);
        }
    }

    @Override
    public void onTypingEnded(Member member)
    {
        if (member != null) {
            TextView typingIndc = (TextView)findViewById(R.id.typingIndicator);
            typingIndc.setText(null);
            logger.d(member.getUserInfo().getIdentity() + " ended typing");
        }
    }

    private void showChangeUniqueNameDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_unique_name, null))
            .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    String uniqueName =
                        ((EditText)editTextDialog.findViewById(R.id.update_unique_name))
                            .getText()
                            .toString();
                    logger.d(uniqueName);

                    channel.setUniqueName(uniqueName, new StatusListener() {
                        @Override
                        public void onError(ErrorInfo errorInfo)
                        {
                            MainApplication.get().showError(errorInfo);
                            MainApplication.get().logErrorInfo(
                                "Error changing channel uniqueName", errorInfo);
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("Successfully changed channel uniqueName");
                        }
                    });
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    @Override
    public void onSynchronizationChange(Channel channel)
    {
        logger.d("Received onSynchronizationChange callback " + channel.getFriendlyName());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivityResultCommon.ACTION_EDIT_GROUP) {
            if (resultCode == ActivityResultCommon.RESULT_LEAVE_GROUP
                    || resultCode == ActivityResultCommon.RESULT_REMOVE_GROUP) {
                this.finish();
            }
        }
    }

    public static class MessageItem
    {
        Message message;
        Members members;
        String  currentUser;

        public MessageItem(Message message, Members members, String currentUser)
        {
            this.message = message;
            this.members = members;
            this.currentUser = currentUser;
        }

        public Message getMessage()
        {
            return message;
        }

        public Members getMembers()
        {
            return members;
        }

        public String getCurrentUser()
        {
            return currentUser;
        }
    }
}
