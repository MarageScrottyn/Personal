package com.android.personal.api.model;

public class UserUpdateRequest {
    private String user_type;
    
    public UserUpdateRequest(String user_type) {
        this.user_type = user_type;
    }
    
    public String getUser_type() {
        return user_type;
    }
    
    public void setUser_type(String user_type) {
        this.user_type = user_type;
    }
}