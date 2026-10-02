package com.android.personal.api;

import java.util.List;

import com.android.personal.api.model.BaseResponse;
import com.android.personal.api.model.Category;
import com.android.personal.api.model.CloudFile;
import com.android.personal.api.model.CloudFileRequest;
import com.android.personal.api.model.Comic;
import com.android.personal.api.model.FolderRequest;
import com.android.personal.api.model.Note;
import com.android.personal.api.model.NoteRequest;
import com.android.personal.api.model.ChangePasswordRequest;
import com.android.personal.api.model.ResetPasswordRequest;
import com.android.personal.api.model.User;
import com.android.personal.api.model.UserUpdateRequest;
import com.android.personal.api.model.Video;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("comics/")
    Call<List<Comic>> getComics();
    
    @GET("comics/{slug}/")
    Call<Comic> getComicDetail(@Path("slug") String slug);
    
    @GET("videos/")
    Call<List<Video>> getVideos();
    
    @GET("videos/{slug}/")
    Call<Video> getVideoDetail(@Path("slug") String slug);
    
    @GET("notes/")
    Call<List<Note>> getNotes();
    
    @POST("notes/")
    Call<Note> createNote(@Body NoteRequest request);
    
    @PUT("notes/{id}/")
    Call<Note> updateNote(@Path("id") int id, @Body NoteRequest request);
    
    @DELETE("notes/{id}/")
    Call<Void> deleteNote(@Path("id") int id);
    
    @GET("cloud/")
    Call<List<CloudFile>> getCloudFiles(@Query("parent") Integer parentId);
    
    @POST("cloud/")
    Call<CloudFile> uploadFile(@Body CloudFileRequest request);
    
    @POST("cloud/create-folder/")
    Call<CloudFile> createFolder(@Body FolderRequest request);
    
    @DELETE("cloud/{id}/")
    Call<Void> deleteFile(@Path("id") int id);
    
    @GET("categories/")
    Call<List<Category>> getCategories();
    
    @POST("sync-media/")
    Call<BaseResponse> syncMedia();
    
    @GET("users/")
    Call<List<User>> getUsers();
    
    @PUT("users/{id}/")
    Call<User> updateUser(@Path("id") int id, @Body UserUpdateRequest request);
    
    @POST("admin/users/{id}/reset-password/")
    Call<BaseResponse> resetPassword(@Path("id") int id, @Body ResetPasswordRequest request);
    
    @POST("change-password/")
    Call<BaseResponse> changePassword(@Body ChangePasswordRequest request);
    
    // 分类管理
    @POST("admin/categories/create/")
    Call<Category> createCategory(@Body Category category);
    
    @PUT("admin/categories/{slug}/update/")
    Call<Category> updateCategory(@Path("slug") String slug, @Body Category category);
    
    @DELETE("admin/categories/{slug}/delete/")
    Call<Void> deleteCategory(@Path("slug") String slug);
    
    // 漫画管理
    @DELETE("admin/comics/{slug}/delete/")
    Call<Void> deleteComic(@Path("slug") String slug);
    
    // 视频管理
    @DELETE("admin/videos/{slug}/delete/")
    Call<Void> deleteVideo(@Path("slug") String slug);
}