package com.mhealth.chat.demo.adapter;

import android.view.View;

import com.twilio.ipmessaging.Member;

/**
 * Created by luhonghai on 8/31/16.
 */

public interface SelectMemberViewListener {
    void selectMember(View view, Member member);
}
