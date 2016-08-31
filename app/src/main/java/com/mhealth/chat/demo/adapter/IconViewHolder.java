package com.mhealth.chat.demo.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.mhealth.chat.demo.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luhonghai on 8/31/16.
 */

public class IconViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.img_group)
    ImageView imageView;

    @Bind(R.id.card_item)
    View cardItem;

    private final IconViewListener listener;

    @OnClick(R.id.card_item)
    public void clickCardItem(View view) {
        if (listener != null)
            listener.selectIcon(view.getTag().toString());
    }

    public IconViewHolder(View itemView, IconViewListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public View getCardItem() {
        return cardItem;
    }

    public void setCardItem(View cardItem) {
        this.cardItem = cardItem;
    }
}
