package com.android.personal.api.model;

public class FolderRequest {
    private String name;
    private Integer parent;
    
    public FolderRequest(String name, Integer parent) {
        this.name = name;
        this.parent = parent;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getParent() {
        return parent;
    }
    
    public void setParent(Integer parent) {
        this.parent = parent;
    }
}