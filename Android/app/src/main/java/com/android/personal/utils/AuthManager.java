package com.android.personal.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.personal.api.model.User;
import com.google.gson.Gson;

public class AuthManager {
    private static final String PREF_NAME = "FloraAuth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER = "user";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    public static final int THEME_MODE_FOLLOW_SYSTEM = 0;
    public static final int THEME_MODE_LIGHT = 1;
    public static final int THEME_MODE_DARK = 2;
    
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static void setAuth(Context context, String accessToken, String refreshToken, User user) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        if (user != null) {
            editor.putString(KEY_USER, new Gson().toJson(user));
        }
        editor.apply();
    }
    
    public static String getAccessToken(Context context) {
        return getPreferences(context).getString(KEY_ACCESS_TOKEN, null);
    }
    
    public static String getRefreshToken(Context context) {
        return getPreferences(context).getString(KEY_REFRESH_TOKEN, null);
    }
    
    public static User getUser(Context context) {
        String userJson = getPreferences(context).getString(KEY_USER, null);
        if (userJson != null) {
            return new Gson().fromJson(userJson, User.class);
        }
        return null;
    }
    
    public static void updateUser(Context context, User user) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(KEY_USER, new Gson().toJson(user));
        editor.apply();
    }
    
    public static boolean isAuthenticated(Context context) {
        String token = getAccessToken(context);
        return token != null && !token.isEmpty();
    }
    
    public static boolean isAdmin(Context context) {
        User user = getUser(context);
        return user != null && "admin".equals(user.getUser_type());
    }
    
    public static void clearAuth(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_USER);
        editor.apply();
    }
    
    public static void setAccessToken(Context context, String accessToken) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.apply();
    }
    
    public static void setRefreshToken(Context context, String refreshToken) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }
    
    public static int getThemeMode(Context context) {
        return getPreferences(context).getInt(KEY_THEME_MODE, THEME_MODE_FOLLOW_SYSTEM);
    }
    
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(KEY_THEME_MODE, mode);
        editor.apply();
    }
}