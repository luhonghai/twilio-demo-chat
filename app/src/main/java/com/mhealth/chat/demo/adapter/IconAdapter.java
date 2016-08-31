package com.mhealth.chat.demo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.mhealth.chat.demo.IconHelper;
import com.mhealth.chat.demo.R;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Created by luhonghai on 8/31/16.
 */

public class IconAdapter extends RecyclerView.Adapter<IconViewHolder> {

    private final String[] icons;

    private final Context context;

    private final IconViewListener listener;

    public IconAdapter(Context context, String[] icons, IconViewListener listener) {
        this.icons = icons;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public IconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IconViewHolder(LayoutInflater.from(context).inflate(R.layout.icon_group_item,
                parent, false), listener);
    }

    @Override
    public void onBindViewHolder(IconViewHolder holder, int position) {
        ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icons[position]), holder.getImageView());
        holder.getCardItem().setTag(icons[position]);
    }

    @Override
    public int getItemCount() {
        return icons.length;
    }
}
