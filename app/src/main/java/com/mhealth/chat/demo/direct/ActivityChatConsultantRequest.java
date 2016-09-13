package com.mhealth.chat.demo.direct;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mhealth.chat.demo.dialog.DialogChatConsultRequest;

/**
 * Created by leanh215 on 9/13/16.
 */
public class ActivityChatConsultantRequest extends AppCompatActivity {

    public static final String ACTION_CONSULTANT_REQUEST = "CHAT_CONSULTANT_REQUEST";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkIntent();
    }

    private void checkIntent() {

        new DialogChatConsultRequest(this);

    }

    private void showRequestDialog() {

    }





}
