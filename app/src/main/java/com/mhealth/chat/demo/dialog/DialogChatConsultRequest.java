package com.mhealth.chat.demo.dialog;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mhealth.chat.demo.MainApplication;
import com.mhealth.chat.demo.R;
import com.mhealth.chat.demo.databinding.DialogChatConsultRequestBinding;
import com.mhealth.chat.demo.direct.MyLog;
import com.mhealth.chat.demo.fcm.FCMSenderService;
import com.mhealth.chat.demo.fcm.NotificationData;
import com.mhealth.chat.demo.fcm.NotificationObject;
import com.mhealth.chat.demo.fcm.data.ChatConsultRejectData;
import com.mhealth.chat.demo.twilio.TwilioClient;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by leanh215 on 9/13/16.
 */
public class DialogChatConsultRequest {

    Context mContext;
    NotificationData mData;
    Handler mCallback;

    MaterialDialog mDialog;
    DialogChatConsultRequestBinding mBinding;

    TwilioClient mTwilioClient;

    public DialogChatConsultRequest(Context context, NotificationData data, Handler callback) {
        mContext = context;
        mTwilioClient = MainApplication.get().getBasicClient();
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
            sendFCMNotificationReject();
            mDialog.dismiss();
            ((AppCompatActivity)mContext).finish();
        });

        mDialog = new MaterialDialog.Builder(mContext).cancelable(false)
                .customView(mBinding.getRoot(), false).build();
        mDialog.show();
    }

    private void sendFCMNotificationReject() {
        FCMSenderService.FCMRequest fcmRequest = new FCMSenderService.FCMRequest.Builder()
                .to(mData.getSenderToken())
                .title(mContext.getString(R.string.title_chat_consult_refused))
                .body(String.format(mContext.getString(R.string.content_chat_consult_refused),
                        mTwilioClient.getIpMessagingClient().getMyUserInfo().getFriendlyName()))
                .type(NotificationObject.Type.CHAT_CONSULT_REJECT)
                .data(new ChatConsultRejectData(mTwilioClient.getIpMessagingClient().getMyUserInfo()))
                .build();

        FCMSenderService.send(fcmRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<FCMSenderService.FCMResponse>() {
                    @Override
                    public void call(FCMSenderService.FCMResponse fcmResponse) {
                        if (fcmResponse.getSuccess() == 0) {
                            MyLog.log("Send FCM Notification CHAT_CONSULT_REJECT failed");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        MyLog.log("Send FCM Notification CHAT_CONSULT_REJECT failed");
                    }
                });
    }

}
