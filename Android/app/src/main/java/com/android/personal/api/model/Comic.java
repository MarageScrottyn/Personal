package com.android.personal.api.model;

import java.util.ArrayList;
import java.util.List;

public class Comic {
    private int id;
    private String title;
    private String slug;
    private String cover_image;
    private String author;
    private String description;
    private String created_at;
    private List<Integer> categories;
    private List<String> category_names;
    private List<ComicChapter> chapters;
    
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
    
    public String getCover_image() {
        return cover_image;
    }
    
    public void setCover_image(String cover_image) {
        this.cover_image = cover_image;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCreated_at() {
        return created_at;
    }
    
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
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
    
    public List<ComicChapter> getChapters() {
        return chapters;
    }
    
    public void setChapters(List<ComicChapter> chapters) {
        this.chapters = chapters;
    }
}