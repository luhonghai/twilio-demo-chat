package com.mhealth.chat.demo.twilio;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mhealth.chat.demo.data.TwilioChannel;
import com.mhealth.chat.demo.data.TwilioMeta;
import com.mhealth.chat.demo.data.TwilioUser;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by luhonghai on 8/31/16.
 */

public class TwilioService {

    private static final String TWILIO_IPM_SERVICE_SID = "ISe9f24d170e3e45ca895661a7e8f00a4f";

    protected TwilioService() {}

    public static TwilioApiService getInstance() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();
                        builder.addHeader("Authorization",
                                getAuthorization());
                        builder.method(original.method(), original.body());
                        return chain.proceed(builder.build());
                    }
                })
                .addInterceptor(interceptor)
                .build();
        Gson gson = new GsonBuilder()
                .setPrettyPrinting().create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://ip-messaging.twilio.com/v1/Services/"+ TWILIO_IPM_SERVICE_SID + "/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build();
        return retrofit.create(TwilioApiService.class);
    }

    private static String getAuthorization() {
//        return "Bearer " + new UserPreference(MainApplication.get()).getAccessToken();
        return "Basic "
                + Base64.encodeToString("AC9f9be1b554229fe50dd8a6f929de0607:f24c3f79a6f4a361b9283f97dea67a3d".getBytes(), Base64.NO_WRAP);
    }

    public interface TwilioApiService {
        @GET("Users")
        Observable<TwilioUsersResponse> listUsers(@Query("PageSize") int pageSize, @Query("Page") int page);

        @GET("Users/{sid}")
        Observable<TwilioUser> getUser(@Path("sid") String sid);

        @GET("Channels")
        Observable<TwilioChannelsResponse> listChannels(@Query("PageSize") int pageSize, @Query("Page") int page);

        @GET("Channels/{sid}")
        Observable<TwilioChannel> getChannel(@Path("sid") String sid);
    }

    public static class TwilioUsersResponse {

        private List<TwilioUser> users;

        public List<TwilioUser> getUsers() {
            return users;
        }

        public void setUsers(List<TwilioUser> users) {
            this.users = users;
        }

    }

    public static class TwilioChannelsResponse {

        private List<TwilioMeta> meta;

        private List<TwilioChannel> channels;

        public List<TwilioMeta> getMeta() {
            return meta;
        }

        public void setMeta(List<TwilioMeta> meta) {
            this.meta = meta;
        }

        public List<TwilioChannel> getChannels() {
            return channels;
        }

        public void setChannels(List<TwilioChannel> channels) {
            this.channels = channels;
        }
    }
}
