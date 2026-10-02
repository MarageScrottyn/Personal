package com.android.personal.api.model;

public class Category {
    private int id;
    private String name;
    private String slug;
    private String permission_level;
    private Integer parent;
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public String getPermission_level() {
        return permission_level;
    }
    
    public void setPermission_level(String permission_level) {
        this.permission_level = permission_level;
    }
    
    public Integer getParent() {
        return parent;
    }
    
    public void setParent(Integer parent) {
        this.parent = parent;
    }
}