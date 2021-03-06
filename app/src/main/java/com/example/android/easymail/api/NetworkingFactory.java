package com.example.android.easymail.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Harshit Bansal on 5/20/2017.
 */

public class NetworkingFactory {

    public static final String BASE_URL = "http://picasaweb.google.com/data/entry/api/user/";
    public static Retrofit retrofit;

    public static Retrofit getClient(){

        if(retrofit == null){
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().
                    addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request originalrequest = chain.request();
                            Request.Builder builder =
                                    originalrequest.newBuilder().header("Authorization",
                                            " ");
                            Request newRequest = builder.build();
                            return chain.proceed(newRequest);
                        }
                    }).build();

            retrofit = new Retrofit.Builder().
                    baseUrl(BASE_URL).
                    addConverterFactory(GsonConverterFactory.create()).
                    build();
        }
        return retrofit;

    }
}

