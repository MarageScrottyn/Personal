package com.android.personal.api.model;

public class ResetPasswordRequest {
    private String new_password;
    
    public ResetPasswordRequest(String new_password) {
        this.new_password = new_password;
    }
    
    public String getNew_password() {
        return new_password;
    }
    
    public void setNew_password(String new_password) {
        this.new_password = new_password;
    }
}