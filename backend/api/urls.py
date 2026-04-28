from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from .views import (
    RegisterView, LoginView, CategoryListView, CategoryDetailView,
    ComicListView, ComicDetailView,
    VideoListView, VideoDetailView,
    # 管理相关视图
    ComicCreateView, ComicUpdateView, ComicDeleteView,
    ComicChapterCreateView, ComicChapterUpdateView, ComicChapterDeleteView,
    VideoCreateView, VideoUpdateView, VideoDeleteView,
    CategoryCreateView, CategoryUpdateView, CategoryDeleteView,
    UserPasswordResetView,
    ImageUploadView, VideoUploadView, VideoThumbnailUploadView, VideoFolderUploadView,
    sync_media_view, cleanup_temp_files,
    # 笔记视图
    NoteListView, NoteDetailView,
    # 用户管理视图
    UserListView, UserUpdateView, change_password_view,
    # 云盘视图
    CloudFileListView, CloudFileDetailView, create_folder_view
)

urlpatterns = [
    path('auth/register/', RegisterView.as_view(), name='register'),
    path('auth/login/', LoginView.as_view(), name='login'),
    path('auth/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('categories/', CategoryListView.as_view(), name='category_list'),
    path('categories/<int:pk>/', CategoryDetailView.as_view(), name='category_detail'),
    path('comics/', ComicListView.as_view(), name='comic_list'),
    path('comics/<slug:slug>/', ComicDetailView.as_view(), name='comic_detail'),
    path('videos/', VideoListView.as_view(), name='video_list'),
    path('videos/<slug:slug>/', VideoDetailView.as_view(), name='video_detail'),

    # 笔记路由
    path('notes/', NoteListView.as_view(), name='note_list'),
    path('notes/<int:pk>/', NoteDetailView.as_view(), name='note_detail'),

    # 管理相关路由
    path('admin/comics/create/', ComicCreateView.as_view(), name='comic_create'),
    path('admin/comics/<slug:slug>/update/', ComicUpdateView.as_view(), name='comic_update'),
    path('admin/comics/<slug:slug>/delete/', ComicDeleteView.as_view(), name='comic_delete'),

    path('admin/comic-chapters/create/', ComicChapterCreateView.as_view(), name='comic_chapter_create'),
    path('admin/comic-chapters/<int:pk>/update/', ComicChapterUpdateView.as_view(), name='comic_chapter_update'),
    path('admin/comic-chapters/<int:pk>/delete/', ComicChapterDeleteView.as_view(), name='comic_chapter_delete'),

    path('admin/videos/create/', VideoCreateView.as_view(), name='video_create'),
    path('admin/videos/<slug:slug>/update/', VideoUpdateView.as_view(), name='video_update'),
    path('admin/videos/<slug:slug>/delete/', VideoDeleteView.as_view(), name='video_delete'),

    path('admin/categories/create/', CategoryCreateView.as_view(), name='category_create'),
    path('admin/categories/<slug:slug>/update/', CategoryUpdateView.as_view(), name='category_update'),
    path('admin/categories/<slug:slug>/delete/', CategoryDeleteView.as_view(), name='category_delete'),

    # 图片上传
    path('upload/image/', ImageUploadView.as_view(), name='image_upload'),
    path('upload/video/', VideoUploadView.as_view(), name='video_upload'),
    path('upload/video-folder/', VideoFolderUploadView.as_view(), name='video_folder_upload'),
    path('upload/video-thumbnail/', VideoThumbnailUploadView.as_view(), name='video_thumbnail_upload'),

    # 同步媒体文件
    path('sync-media/', sync_media_view, name='sync_media'),
    # 清理临时文件
    path('cleanup-temp/', cleanup_temp_files, name='cleanup_temp'),
    
    # 用户管理路由
    path('users/', UserListView.as_view(), name='user_list'),
    path('users/<int:pk>/', UserUpdateView.as_view(), name='user_update'),
    path('change-password/', change_password_view, name='change_password'),
    path('admin/users/<int:user_id>/reset-password/', UserPasswordResetView.as_view(), name='user_password_reset'),

    # 云盘路由
    path('cloud/', CloudFileListView.as_view(), name='cloud_list'),
    path('cloud/<int:id>/', CloudFileDetailView.as_view(), name='cloud_detail'),
    path('cloud/create-folder/', create_folder_view, name='cloud_create_folder'),
]
