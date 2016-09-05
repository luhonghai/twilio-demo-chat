package com.mhealth.chat.demo;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.util.DatetimeUtils;
import com.mhealth.chat.demo.util.DrawableUtils;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;
import com.twilio.ipmessaging.UserInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import uk.co.ribot.easyadapter.ItemViewHolder;
import uk.co.ribot.easyadapter.PositionInfo;
import uk.co.ribot.easyadapter.annotations.LayoutId;
import uk.co.ribot.easyadapter.annotations.ViewId;

@LayoutId(R.layout.message_item_layout)
public class MessageViewHolder extends ItemViewHolder<MessageActivity.MessageItem>
{

    private static final long MESSAGE_TIMESTAMP_RANGE = 2 * 60 * 60 * 1000; // 2 hours

    private static int[] HORIZON_COLORS = {
        Color.GRAY, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA
    };

    @ViewId(R.id.avatar)
    SimpleDraweeView imageView;

    @ViewId(R.id.reachability)
    SimpleDraweeView reachabilityView;

    @ViewId(R.id.body)
    TextView body;

    @ViewId(R.id.txt_author)
    TextView author;

    @ViewId(R.id.txt_timestamp)
    TextView timestamp;

    @ViewId(R.id.chat)
    View viewChat;

    @ViewId(R.id.consumptionHorizonIdentities)
    LinearLayout identities;

    @ViewId(R.id.message_container)
    View messageContainer;

    @ViewId(R.id.message_card_view)
    CardView messageCardView;

    @ViewId(R.id.avatar_container)
    View avatarContainer;

    @ViewId(R.id.message_status)
    View messageStatus;

    View view;

    LayoutInflater inflater;

    public interface MessageItemAdapter {
        MessageActivity.MessageItem getMessageItemByPosition(int pos);
    }

    public MessageViewHolder(View view)
    {
        super(view);
        this.view = view;
        inflater = LayoutInflater.from(getContext());
    }

    @Override
    public void onSetListeners()
    {
        messageCardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v)
            {
                OnMessageClickListener listener = getListener(OnMessageClickListener.class);
                if (listener != null) {
                    listener.onMessageClicked(getItem());
                    return true;
                }
                return false;
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnMessageClickListener listener = getListener(OnMessageClickListener.class);
                if (listener != null) {
                    listener.onMemberSelect((Member) v.getTag());
                }
            }
        });
    }

    @Override
    public void onSetValues(MessageActivity.MessageItem message, PositionInfo pos)
    {
        if (message != null) {
            Message msg = message.getMessage();

            body.setText(msg.getMessageBody());

            identities.removeAllViews();

            if (message.getMembers() != null && message.getMembers().getMembers() != null) {
                boolean isReaded = false;
                for (Member member : message.getMembers().getMembers()) {
                    if (member.getLastConsumedMessageIndex() != null
                            && member.getLastConsumedMessageIndex()
                            == message.getMessage().getMessageIndex()
                            && !member.getUserInfo().getIdentity().equalsIgnoreCase(message.getCurrentUser())) {
                        drawConsumptionHorizon(member);
                    }
                    if (member.getLastConsumedMessageIndex() != null
                            && member.getLastConsumedMessageIndex()
                            >= message.getMessage().getMessageIndex()
                            && !member.getUserInfo().getIdentity().equalsIgnoreCase(message.getCurrentUser())) {
                        isReaded = true;
                    }
                    if (msg.getAuthor().equals(member.getUserInfo().getIdentity())) {
                        DrawableUtils.fillUserAvatar(imageView, member);
                        DrawableUtils.fillUserReachability(reachabilityView, member);
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) avatarContainer.getLayoutParams();
                        RelativeLayout.LayoutParams mesParams = (RelativeLayout.LayoutParams) messageContainer.getLayoutParams();
                        if (msg.getAuthor().equalsIgnoreCase(message.getCurrentUser())) {
                            params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                            mesParams.removeRule(RelativeLayout.END_OF);
                            mesParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                            messageCardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.colorPrimary));
                            avatarContainer.setVisibility(View.GONE);
                            body.setTextColor(getContext().getResources().getColor(android.R.color.white));
                        } else {
                            params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
                            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                            mesParams.addRule(RelativeLayout.END_OF, R.id.avatar_container);
                            mesParams.removeRule(RelativeLayout.ALIGN_PARENT_END);
                            messageCardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.message_background_gray));
                            avatarContainer.setVisibility(View.VISIBLE);
                            body.setTextColor(getContext().getResources().getColor(android.R.color.black));
                        }
                        messageContainer.setLayoutParams(mesParams);
                        avatarContainer.setLayoutParams(params);
                        if (isLastAuthorMessage(view.getContext(), message, pos)
                                && !msg.getAuthor().equalsIgnoreCase(message.getCurrentUser())) {
                            avatarContainer.setVisibility(View.VISIBLE);
                        } else {
                            avatarContainer.setVisibility(View.INVISIBLE);
                        }
                        if (isFirstAuthorMessage(view.getContext(), message, pos)
                                && !msg.getAuthor().equalsIgnoreCase(message.getCurrentUser())) {
                            author.setVisibility(View.VISIBLE);
                            author.setText(getMemberName(member.getUserInfo()));
                        } else {
                            author.setVisibility(View.GONE);
                        }
                        if ((isFirstAuthorMessage(view.getContext(), message, pos)
                                && !isLastAuthorMessage(view.getContext(), message, pos))
                                || isVisibleMessage(view.getContext(), message, pos)) {
                            timestamp.setVisibility(View.VISIBLE);
                            timestamp.setText(getTimestamp(message));
                        } else {
                            timestamp.setVisibility(View.GONE);
                        }
                    }
                }
                messageStatus.setVisibility((isReaded || !msg.getAuthor().equalsIgnoreCase(message.getCurrentUser()) ?
                        View.INVISIBLE : View.VISIBLE));
            }
        }
    }

    private boolean isVisibleMessage(Context context, MessageActivity.MessageItem messageItem, PositionInfo pos) {
        if (pos.isFirst()) return true;
        if (context instanceof MessageItemAdapter) {
            MessageItemAdapter adapter = (MessageItemAdapter) context;
            MessageActivity.MessageItem prevItem = adapter.getMessageItemByPosition(pos.getPosition() - 1);
            if (prevItem != null) {
                try {
                    Date mCurrentDate = DatetimeUtils.getMessageTimestamp(messageItem.getMessage());
                    Date mPrevDate = DatetimeUtils.getMessageTimestamp(prevItem.getMessage());
                    return  ((mCurrentDate.getTime() - mPrevDate.getTime()) >= MESSAGE_TIMESTAMP_RANGE);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } else {
            Log.d("MessageViewHolder", "isFirstAuthorMessage: context do not implement MessageItemAdapter");
        }
        return false;
    }

    private String getTimestamp(MessageActivity.MessageItem messageItem) {
        String timestamp = messageItem.getMessage().getTimeStamp();
        try {
            Date date = DatetimeUtils.getMessageTimestamp(messageItem.getMessage());
            Calendar mCal = Calendar.getInstance();
            mCal.setTime(date);
            Calendar today = Calendar.getInstance();
            today.setTimeInMillis(System.currentTimeMillis());
            SimpleDateFormat sdfOut;
            if (mCal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && mCal.get(Calendar.DATE) == today.get(Calendar.DATE)
                    && mCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                sdfOut  = new SimpleDateFormat("HH:mm", Locale.US);
            } else if (mCal.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
                sdfOut =  new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            } else {
                sdfOut = new SimpleDateFormat("dd MMM, HH:mm", Locale.US);
            }
            return sdfOut.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    private String getMemberName(UserInfo userInfo) {
        return (userInfo.getFriendlyName() != null && !userInfo.getFriendlyName().isEmpty())
                ?  userInfo.getFriendlyName() : userInfo.getIdentity();
    }

    private boolean isLastAuthorMessage(Context context, MessageActivity.MessageItem current, PositionInfo pos) {
        if (pos.isLast()) return true;
        if (context instanceof MessageItemAdapter) {
            MessageItemAdapter adapter = (MessageItemAdapter) context;
            MessageActivity.MessageItem nextItem = adapter.getMessageItemByPosition(pos.getPosition() + 1);
            return (nextItem != null
                    && !nextItem.getMessage().getAuthor().equalsIgnoreCase(current.getMessage().getAuthor()));
        } else {
            Log.d("MessageViewHolder", "isLastAuthorMessage: context do not implement MessageItemAdapter");
        }
        return false;
    }

    private boolean isFirstAuthorMessage(Context context, MessageActivity.MessageItem current, PositionInfo pos) {
        if (pos.isFirst()) return true;
        if (context instanceof MessageItemAdapter) {
            MessageItemAdapter adapter = (MessageItemAdapter) context;
            MessageActivity.MessageItem prevItem = adapter.getMessageItemByPosition(pos.getPosition() - 1);
            return (prevItem != null
                    && !prevItem.getMessage().getAuthor().equalsIgnoreCase(current.getMessage().getAuthor()));
        } else {
            Log.d("MessageViewHolder", "isFirstAuthorMessage: context do not implement MessageItemAdapter");
        }
        return false;
    }

    private void drawConsumptionHorizon(Member member)
    {
        SimpleDraweeView view = (SimpleDraweeView)
                inflater.inflate(R.layout.small_member_avatar_item, identities, false);
        DrawableUtils.fillUserAvatar(view, member);
        identities.addView(view);
    }


    public interface OnMessageClickListener {
        void onMessageClicked(MessageActivity.MessageItem message);
        void onMemberSelect(Member member);
    }

    public int getMemberRgb(String identity)
    {
        return HORIZON_COLORS[Math.abs(identity.hashCode()) % HORIZON_COLORS.length];
    }
}
