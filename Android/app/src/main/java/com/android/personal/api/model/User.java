package com.android.personal.api.model;

import java.util.List;

public class User {
    private int id;
    private String username;
    private String email;
    private String user_type;
    private Profile profile;
    private List<String> permissions;
    private List<String> can_access_comic_categories;
    private List<String> can_access_video_categories;
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUser_type() {
        return user_type;
    }
    
    public void setUser_type(String user_type) {
        this.user_type = user_type;
    }
    
    public Profile getProfile() {
        return profile;
    }
    
    public void setProfile(Profile profile) {
        this.profile = profile;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    public List<String> getCan_access_comic_categories() {
        return can_access_comic_categories;
    }
    
    public void setCan_access_comic_categories(List<String> can_access_comic_categories) {
        this.can_access_comic_categories = can_access_comic_categories;
    }
    
    public List<String> getCan_access_video_categories() {
        return can_access_video_categories;
    }
    
    public void setCan_access_video_categories(List<String> can_access_video_categories) {
        this.can_access_video_categories = can_access_video_categories;
    }
    
    public static class Profile {
        private String user_type;
        
        public String getUser_type() {
            return user_type;
        }
        
        public void setUser_type(String user_type) {
            this.user_type = user_type;
        }
    }
}