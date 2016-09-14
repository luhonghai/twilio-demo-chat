package com.mhealth.chat.demo.dialog;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.databinding.DialogChatConsultRequestBinding;
import com.mhealth.chat.demo.fcm.NotificationData;

/**
 * Created by leanh215 on 9/13/16.
 */
public class DialogChatConsultRequest {

    Context mContext;
    NotificationData mData;
    Handler mCallback;

    MaterialDialog mDialog;
    DialogChatConsultRequestBinding mBinding;

    public DialogChatConsultRequest(Context context, NotificationData data, Handler callback) {
        mContext = context;
        mData = data;
        mCallback = callback;
        initUI();
    }

    private void initUI() {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_chat_consult_request, null, false);
        mBinding.tvRequestBody.setText(String.format(mContext.getString(R.string.chat_consult_request_content),
                mData.getSenderFriendlyName()));
        mBinding.btnAccept.setOnClickListener(view -> {
            mCallback.sendEmptyMessage(0);
            mDialog.dismiss();

        });
        mBinding.btnDeny.setOnClickListener(view -> {
            mDialog.dismiss();
            ((AppCompatActivity)mContext).finish();
        });

        mDialog = new MaterialDialog.Builder(mContext).cancelable(false)
                .customView(mBinding.getRoot(), false).build();
        mDialog.show();
    }

}
