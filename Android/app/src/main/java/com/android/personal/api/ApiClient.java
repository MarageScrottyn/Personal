package com.android.personal.api;

import android.content.Context;

import com.android.personal.api.model.LoginResponse;
import com.android.personal.api.model.RefreshRequest;
import com.android.personal.utils.AuthManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // 服务器地址：使用 Cloudflare 域名（HTTPS）
    public static final String BASE_URL = "https://marage.ccwu.cc/";
    private static final String API_URL = BASE_URL + "api/";
    
    private static Retrofit retrofit;
    private static ApiService apiService;
    private static AuthService authService;
    
    private static OkHttpClient getOkHttpClient(Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .addInterceptor(new LoggingInterceptor())
                .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }
    
    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long t1 = System.nanoTime();
            android.util.Log.d("ApiClient", "Sending request: " + request.url());
            
            Response response = chain.proceed(request);
            
            long t2 = System.nanoTime();
            android.util.Log.d("ApiClient", "Received response for " + request.url() 
                    + " in " + (t2 - t1) / 1e6 + "ms"
                    + " code: " + response.code());
            
            return response;
        }
    }
    
    private static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .client(getOkHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = getRetrofit(context).create(ApiService.class);
        }
        return apiService;
    }
    
    public static AuthService getAuthService(Context context) {
        if (authService == null) {
            authService = getRetrofit(context).create(AuthService.class);
        }
        return authService;
    }
    
    private static class AuthInterceptor implements Interceptor {
        private Context context;
        
        public AuthInterceptor(Context context) {
            this.context = context;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            String token = AuthManager.getAccessToken(context);
            
            Request.Builder requestBuilder = original.newBuilder();
            if (token != null && !token.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }
            
            Request request = requestBuilder.build();
            Response response = chain.proceed(request);
            
            if (response.code() == 401) {
                String refreshToken = AuthManager.getRefreshToken(context);
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    try {
                        retrofit2.Response<LoginResponse> refreshResponse = getRetrofit(context)
                                .create(AuthService.class)
                                .refreshToken(new RefreshRequest(refreshToken))
                                .execute();
                        
                        if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                            String newAccessToken = refreshResponse.body().getAccess();
                            AuthManager.setAccessToken(context, newAccessToken);
                            
                            Request newRequest = original.newBuilder()
                                    .header("Authorization", "Bearer " + newAccessToken)
                                    .build();
                            return chain.proceed(newRequest);
                        }
                    } catch (Exception e) {
                        AuthManager.clearAuth(context);
                    }
                } else {
                    AuthManager.clearAuth(context);
                }
            }
            
            return response;
        }
    }
}