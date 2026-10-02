package com.android.personal.api;

import com.android.personal.api.model.BaseResponse;
import com.android.personal.api.model.ChangePasswordRequest;
import com.android.personal.api.model.LoginRequest;
import com.android.personal.api.model.LoginResponse;
import com.android.personal.api.model.RefreshRequest;
import com.android.personal.api.model.RegisterRequest;
import com.android.personal.api.model.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("auth/login/")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    @POST("auth/register/")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    
    @POST("auth/refresh/")
    Call<LoginResponse> refreshToken(@Body RefreshRequest request);
    
    @POST("change-password/")
    Call<BaseResponse> changePassword(@Body ChangePasswordRequest request);
}