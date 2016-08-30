package com.mhealth.chat.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.ErrorInfo;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddGroupActivity extends AppCompatActivity {

    @Bind(R.id.txt_name)
    TextView txtName;

    @Bind(R.id.switch_type)
    Switch switchType;

    @Bind(R.id.img_group)
    ImageView imageView;

    BasicIPMessagingClient chatClient;

    private String selectedIcon = "";

    MaterialDialog dialogIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        chatClient = TwilioApplication.get().getBasicClient();
        ButterKnife.bind(this);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @OnClick(R.id.img_group)
    public void clickSelectIcon() {
        try {
            dialogIcons = new MaterialDialog.Builder(this)
                    .title("Select a group icon")
                    .negativeText("Cancel")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .adapter(new IconAdapter(getAssets().list("medical-icons")), new GridLayoutManager(this, 4))
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectIcon(String icon) {
        if (dialogIcons != null && dialogIcons.isShowing()) {
            dialogIcons.dismiss();
        }
        selectedIcon = icon;
        ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icon), imageView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogIcons != null && dialogIcons.isShowing()) {
            dialogIcons.dismiss();
        }
    }

    @OnClick(R.id.btn_add)
    public void clickAdd() {
        if (txtName.getText().toString().isEmpty()) return;
        chatClient.getIpMessagingClient().getChannels()
                .createChannel(txtName.getText().toString(),
                        switchType.isChecked() ? Channel.ChannelType.PRIVATE
                            : Channel.ChannelType.PUBLIC,
                        new Constants.CreateChannelListener() {
            @Override
            public void onCreated(final Channel channel) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("group_icon", selectedIcon);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                channel.setAttributes(jsonObject, new Constants.StatusListener() {
                    @Override
                    public void onSuccess() {
                        channel.synchronize(new Constants.CallbackListener<Channel>() {
                            @Override
                            public void onSuccess(Channel channel) {
                                EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.GROUP_ADDED));
                                close();
                            }
                        });
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        super.onError(errorInfo);
                    }
                });
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
            }
        });
    }

    private void close() {
        setResult(RESULT_OK);
        this.finish();
    }

    class IconAdapter extends RecyclerView.Adapter<IconViewHolder> {

        private final String[] icons;

        public IconAdapter(String[] icons) {
            this.icons = icons;
        }

        @Override
        public IconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new IconViewHolder(LayoutInflater.from(AddGroupActivity.this).inflate(R.layout.icon_group_item,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(IconViewHolder holder, int position) {
            ImageLoader.getInstance().displayImage(IconHelper.getGroupIconUrl(icons[position]), holder.imageView);
            holder.cardItem.setTag(icons[position]);
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }
    }

    class IconViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.img_group)
        ImageView imageView;

        @Bind(R.id.card_item)
        View cardItem;

        @OnClick(R.id.card_item)
        public void clickCardItem(View view) {
            selectIcon(view.getTag().toString());
        }

        public IconViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
