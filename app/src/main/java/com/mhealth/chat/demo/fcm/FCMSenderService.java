package com.mhealth.chat.demo.fcm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mhealth.chat.demo.Logger;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.UserInfo;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

/**
 * Created by luhonghai on 9/13/16.
 */

public class FCMSenderService {

    private static final Logger logger = Logger.getLogger(FCMSenderService.class);

    private static final String FCM_TOKEN = "fcm_token";

    protected FCMSenderService() {}

    public static void saveFcmToken(UserInfo userInfo, String token) {
        saveFcmToken(userInfo, token, null);
    }

    public static String getFcmToken(UserInfo userInfo) {
        try {
            JSONObject jsonObject = userInfo.getAttributes();
            if (jsonObject != null && jsonObject.has(FCM_TOKEN)) {
                return jsonObject.optString(FCM_TOKEN);
            }
        } catch (Exception e) {}
        return "";
    }

    public static void saveFcmToken(UserInfo userInfo, String token, Constants.StatusListener statusListener) {
        JSONObject jsonObject = userInfo.getAttributes();
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        try {
            if (jsonObject.has(FCM_TOKEN)) {
                jsonObject.remove(FCM_TOKEN);
            }
            jsonObject.put(FCM_TOKEN, token);
        } catch (Exception e) {}
        if (statusListener == null) {
            statusListener = new Constants.StatusListener() {
                @Override
                public void onSuccess() {

                }
            };
        }
        userInfo.setAttributes(jsonObject, statusListener);
    }

    public static Observable<FCMResponse> send(FCMRequest request) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();
                        builder.addHeader("Authorization",
                                "key=AIzaSyAwBdqUuMP4cIc15VmuhXSbcnt6BYoVv1w");
                        builder.method(original.method(), original.body());
                        return chain.proceed(builder.build());
                    }
                })
                .addInterceptor(interceptor)
                .build();
        Gson gson = new GsonBuilder()
                .setPrettyPrinting().create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://fcm.googleapis.com/fcm/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build();
        FCMService fcmService = retrofit.create(FCMService.class);
        if (logger.isDebugEnabled()) {
            logger.d("FCM request: " + new GsonBuilder().setPrettyPrinting().create().toJson(request));
        }
        return fcmService.send(request);
    }

    public static class FCMResponse {
        private int success;

        private int failure;

        @SerializedName("multicast_id")
        private double multicastId;

        public FCMResponse() {
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailure() {
            return failure;
        }

        public void setFailure(int failure) {
            this.failure = failure;
        }

        public double getMulticastId() {
            return multicastId;
        }

        public void setMulticastId(int multicastId) {
            this.multicastId = multicastId;
        }
    }

    public static class FCMRequest {

        public static class Builder {
            private String to;
            private String sound = "default";
            private String body;
            private String title;
            private NotificationObject.Type type;
            private NotificationData data;

            public FCMRequest build() {
                FCMData fcmData = new FCMData();
                fcmData.type = type.getName();
                fcmData.data = new Gson().toJson(data);
                FCMNotification fcmNotification = new FCMNotification();
                fcmNotification.body = body;
                fcmNotification.title = title;
                fcmNotification.sound = sound;
                fcmNotification.clickAction = fcmData.type;
                FCMRequest request = new FCMRequest(to, fcmData, fcmNotification);
                return request;
            }

            public Builder to(Member member) {
                return to(member.getUserInfo());
            }

            public Builder to(UserInfo userInfo) {
                return to(getFcmToken(userInfo));
            }

            public Builder to(String to) {
                this.to = to;
                return this;
            }

            public Builder body(String body) {
                this.body = body;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder sound(String sound) {
                this.sound = sound;
                return this;
            }

            public Builder type(NotificationObject.Type type) {
                this.type = type;
                return this;
            }

            public Builder data(NotificationData data) {
                this.data = data;
                return this;
            }
        }

        String to;
        FCMData data;
        FCMNotification notification;

        private FCMRequest(String to, FCMData data, FCMNotification fcmNotification) {
            this.to = to;
            this.data = data;
            this.notification = fcmNotification;
        }
    }

    private static class FCMData {
        String data;
        String type;
    }

    private static class FCMNotification {
        String title;
        String body;
        String sound;
        @SerializedName("click_action")
        String clickAction;
    }

    public interface FCMService {
        @POST("send")
        Observable<FCMResponse> send(@Body FCMRequest request);
    }
}
