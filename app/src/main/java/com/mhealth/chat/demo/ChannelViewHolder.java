package com.mhealth.chat.demo;

import com.mhealth.chat.demo.data.TwilioChannel;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Channel.ChannelStatus;
import com.twilio.ipmessaging.Constants;

import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;

import uk.co.ribot.easyadapter.ItemViewHolder;
import uk.co.ribot.easyadapter.PositionInfo;
import uk.co.ribot.easyadapter.annotations.LayoutId;
import uk.co.ribot.easyadapter.annotations.ViewId;

@LayoutId(R.layout.channel_item_layout)
public class ChannelViewHolder extends ItemViewHolder<Channel>
{
    @ViewId(R.id.channel_friendly_name)
    TextView friendlyName;

    @ViewId(R.id.img_group)
    ImageView imageView;

    View view;

    public ChannelViewHolder(View view)
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
                OnChannelClickListener listener = getListener(OnChannelClickListener.class);
                if (listener != null) {
                    listener.onChannelClicked(getItem());
                }
            }
        });
    }

    @Override
    public void onSetValues(Channel channel, PositionInfo arg1)
    {
        TwilioChannel twilioChannel = MainApplication.get().getChannelDataPreference().get(channel.getSid());
        if (twilioChannel != null) {
            friendlyName.setText(twilioChannel.getFriendlyName());
            TwilioChannel.Attribute attrObject = twilioChannel.getAttributeObject();
            if (attrObject != null) {
                ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(attrObject.getIcon()), imageView);
            }
        } else {
            friendlyName.setText(channel.getFriendlyName());
            try {
                String icon = "";
                try {
                    icon = channel.getAttributes().opt("group_icon").toString();
                } catch (Exception e) {
                }
                if (icon != null && icon.length() > 0) {
                    ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icon), imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int color;
        if (channel.getStatus() == ChannelStatus.JOINED) {
            color = getContext().getResources().getColor(R.color.colorPrimary);
        } else {
            color = getContext().getResources().getColor(R.color.colorGray);
        }
        imageView.setColorFilter(color);
        friendlyName.setTextColor(color);
    }

    public interface OnChannelClickListener {
        void onChannelClicked(Channel channel);
    }
}
