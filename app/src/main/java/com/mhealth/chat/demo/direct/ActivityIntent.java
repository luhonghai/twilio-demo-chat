package com.mhealth.chat.demo.direct;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by leanh215 on 8/31/16.
 */
public class ActivityIntent extends AppCompatActivity {

    public static final String FRAGMENT_CHAT_ONE_ONE = "FRAGMENT_CHAT_ONE_ONE";
    public static final String EXTRA_CHAT_TO_ID = "EXTRA_CHAT_TO_ID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, FragmentChatRecent.getInstance())
                .commit();

    }


}
