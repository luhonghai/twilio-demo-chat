package com.mhealth.chat.demo.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.mhealth.chat.demo.R;
import com.twilio.ipmessaging.Member;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luhonghai on 8/31/16.
 */

public class SelectMemberViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.txt_name)
    TextView memberName;

    @Bind(R.id.view_admin)
    View viewAdmin;

    @Bind(R.id.avatar)
    SimpleDraweeView imageView;

    @Bind(R.id.reachability)
    SimpleDraweeView reachabilityView;

    @Bind(R.id.card_item)
    View cardItem;

    @OnClick(R.id.card_item)
    public void clickItem(View item) {
        if (listener != null) {
            listener.selectMember((Member) item.getTag());
        }
    }

    SelectMemberViewListener listener;

    public SelectMemberViewHolder(View itemView, SelectMemberViewListener listener) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.listener = listener;
    }

}
