package com.mhealth.chat.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.adapter.IconAdapter;
import com.mhealth.chat.demo.adapter.IconViewListener;
import com.mhealth.chat.demo.data.TwilioChannel;
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

public class AddGroupActivity extends BaseActivity implements IconViewListener {

    @Bind(R.id.txt_name)
    TextView txtName;

    @Bind(R.id.switch_type)
    Switch switchType;

    @Bind(R.id.img_group)
    ImageView imageView;

    TwilioClient chatClient;

    private String selectedIcon = "";

    MaterialDialog dialogIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        chatClient = MainApplication.get().getBasicClient();
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
                    .adapter(new IconAdapter(this, getAssets().list("medical-icons"), this), new GridLayoutManager(this, 4))
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void selectIcon(String icon) {
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
                            public void onSuccess(final Channel channel) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TwilioChannel.sync(channel);
                                        EventBus.getDefault().post(new ActionEvent(ActionEvent.Action.GROUP_ADDED));
                                        close();
                                    }
                                });
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

}
