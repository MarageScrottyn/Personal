package com.android.personal.api.model;

public class CloudFile {
    private int id;
    private String name;
    private String file_type;
    private String formatted_size;
    private String created_at;
    private String file_url;
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
    
    public String getFile_type() {
        return file_type;
    }
    
    public void setFile_type(String file_type) {
        this.file_type = file_type;
    }
    
    public String getFormatted_size() {
        return formatted_size;
    }
    
    public void setFormatted_size(String formatted_size) {
        this.formatted_size = formatted_size;
    }
    
    public String getCreated_at() {
        return created_at;
    }
    
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
    
    public String getFile_url() {
        return file_url;
    }
    
    public void setFile_url(String file_url) {
        this.file_url = file_url;
    }
    
    public Integer getParent() {
        return parent;
    }
    
    public void setParent(Integer parent) {
        this.parent = parent;
    }
}