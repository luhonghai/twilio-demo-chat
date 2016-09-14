package com.mhealth.chat.demo.direct;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

import com.mhealth.chat.demo.BaseActivity;
import com.mhealth.chat.demo.dialog.DialogChatConsultRequest;
import com.mhealth.chat.demo.fcm.ChatConsultRequestData;
import com.mhealth.chat.demo.fcm.NotificationObject;

/**
 * Created by leanh215 on 9/13/16.
 */
public class ActivityChatConsultantRequest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onNotificationObjectFound(NotificationObject object) {
        super.onNotificationObjectFound(object);

        if (object.getType() == NotificationObject.Type.CHAT_CONSULT_REQUEST) {
            showChatConsultRequest(object);
        }
    }

    private void showChatConsultRequest(NotificationObject object) {
        ChatConsultRequestData data = (ChatConsultRequestData) object.getData();
        new DialogChatConsultRequest(this, data, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // handle accept request
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(android.R.id.content, FragmentChatOneOne.getInstance(data))
                        .commit();
            }
        });
    }
}
