package com.android.personal.api.model;

import java.util.ArrayList;
import java.util.List;

public class Video {
    private int id;
    private String title;
    private String slug;
    private String thumbnail;
    private String video_file;
    private String description;
    private String duration;
    private List<Integer> categories = new ArrayList<>();
    private List<String> category_names = new ArrayList<>();
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public String getThumbnail() {
        return thumbnail;
    }
    
    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
    
    public String getVideo_file() {
        return video_file;
    }
    
    public void setVideo_file(String video_file) {
        this.video_file = video_file;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Integer> getCategories() {
        return categories;
    }
    
    public void setCategories(List<Integer> categories) {
        this.categories = categories;
    }
    
    public List<String> getCategory_names() {
        return category_names;
    }
    
    public void setCategory_names(List<String> category_names) {
        this.category_names = category_names;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
}