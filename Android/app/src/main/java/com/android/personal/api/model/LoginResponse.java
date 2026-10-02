package com.android.personal.api.model;

public class LoginResponse {
    private String access;
    private String refresh;
    private User user;
    
    public String getAccess() {
        return access;
    }
    
    public void setAccess(String access) {
        this.access = access;
    }
    
    public String getRefresh() {
        return refresh;
    }
    
    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
}