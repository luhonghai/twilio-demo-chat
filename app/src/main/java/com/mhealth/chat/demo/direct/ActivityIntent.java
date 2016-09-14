package com.mhealth.chat.demo.direct;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by leanh215 on 8/31/16.
 */
public class ActivityIntent extends AppCompatActivity {

    public static final String EXTRA_FRAGMENT_NAME = "EXTRA_FRAGMENT_NAME";

    public static final String FRAGMENT_CHAT_ONE_ONE = "FRAGMENT_CHAT_ONE_ONE";
    public static final String EXTRA_CHAT_TO_ID = "EXTRA_CHAT_TO_ID";
    public static final String EXTRA_CHAT_TO_FRIENDLY_NAME = "EXTRA_CHAT_TO_FRIENDLY_NAME";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Fragment targetFragment = null;
            String fragmentName = getIntent().getStringExtra(EXTRA_FRAGMENT_NAME);
            if (fragmentName.equals(FRAGMENT_CHAT_ONE_ONE)) {
                String userId = getIntent().getStringExtra(EXTRA_CHAT_TO_ID);
                String username = getIntent().getStringExtra(EXTRA_CHAT_TO_FRIENDLY_NAME);
//                targetFragment = FragmentChatOneOne.getInstance(userId, username);
            }

            if (targetFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .add(android.R.id.content, targetFragment)
                        .commit();
            } else {
                finish();
            }
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, FragmentChatRecent.getInstance())
                    .commit();
        }

    }




}
