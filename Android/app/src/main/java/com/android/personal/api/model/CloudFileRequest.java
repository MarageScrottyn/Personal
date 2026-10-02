package com.android.personal.api.model;

public class CloudFileRequest {
    private String name;
    private String file_type;
    private Integer parent;
    
    public CloudFileRequest(String name, String file_type, Integer parent) {
        this.name = name;
        this.file_type = file_type;
        this.parent = parent;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFile_type() {
        return file_type;
    }
    
    public void setFile_type(String file_type) {
        this.file_type = file_type;
    }
    
    public Integer getParent() {
        return parent;
    }
    
    public void setParent(Integer parent) {
        this.parent = parent;
    }
}