package com.mhealth.chat.demo.direct;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by leanh215 on 8/31/16.
 */
public class ActivityIntent extends AppCompatActivity {

    public static final String EXTRA_FRAGMENT_NAME = "EXTRA_FRAGMENT_NAME";
    public static final String FRAGMENT_CHAT_LIST = "FRAGMENT_CHAT_LIST";

    public static final String FRAGMENT_CHAT_DIRECT = "FRAGMENT_CHAT_DIRECT";
    public static final String EXTRA_CHAT_TO_ID = "EXTRA_CHAT_TO_ID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, FragmentChatUserList.getInstance())
                .commit();

    }


}
