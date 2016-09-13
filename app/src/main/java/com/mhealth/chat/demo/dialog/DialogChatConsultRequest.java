package com.mhealth.chat.demo.dialog;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.databinding.DialogChatConsultRequestBinding;

/**
 * Created by leanh215 on 9/13/16.
 */
public class DialogChatConsultRequest {

    Context mContext;
    MaterialDialog mDialog;
    DialogChatConsultRequestBinding mBinding;

    public DialogChatConsultRequest(Context mContext) {
        this.mContext = mContext;
        initUI();
    }

    private void initUI() {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_chat_consult_request, null, false);
        mDialog = new MaterialDialog.Builder(mContext)
                .customView(mBinding.getRoot(), false).build();
        mDialog.show();
        mDialog.setOnDismissListener(dialogInterface -> {
            ((AppCompatActivity)mContext).finish();
        });
    }


}
