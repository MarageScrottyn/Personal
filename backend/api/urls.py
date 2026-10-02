from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from .views import (
    RegisterView, LoginView, CategoryListView, CategoryDetailView,
    ComicListView, ComicDetailView,
    VideoListView, VideoDetailView,
    ComicCreateView, ComicUpdateView, ComicDeleteView,
    ComicUploadCoverView, ComicChapterUploadImagesView,
    ComicChapterReorderImagesView, AlbumReorderImagesView,
    ComicChapterCreateView, ComicChapterUpdateView, ComicChapterDeleteView,
    VideoCreateView, VideoUpdateView, VideoDeleteView,
    CategoryCreateView, CategoryUpdateView, CategoryDeleteView,
    UserPasswordResetView,
    ImageUploadView, VideoUploadView, VideoThumbnailUploadView, VideoFolderUploadView,
    sync_media_view, cleanup_temp_files,
    NoteListView, NoteDetailView,
    UserListView, UserUpdateView, change_password_view,
    UserProfileView, UserStatsView, UserElevationView,
    CloudFileListView, CloudFileDetailView, create_folder_view,
    ImageListView, ImageDetailView, ImageCreateView, ImageUpdateView, ImageDeleteView,
    ImageBatchUploadView,
    AlbumListView, AlbumDetailView, AlbumCreateView, AlbumUpdateView, AlbumDeleteView,
    AlbumUploadImagesView
)
from .chat_views import chat, chat_stream

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

    path('images/', ImageListView.as_view(), name='image_list'),
    path('images/<int:pk>/', ImageDetailView.as_view(), name='image_detail'),
    path('images/create/', ImageCreateView.as_view(), name='image_create'),
    path('images/<int:pk>/update/', ImageUpdateView.as_view(), name='image_update'),
    path('images/<int:pk>/delete/', ImageDeleteView.as_view(), name='image_delete'),
    path('admin/images/batch-upload/', ImageBatchUploadView.as_view(), name='image_batch_upload'),

    path('albums/', AlbumListView.as_view(), name='album_list'),
    path('albums/<int:pk>/', AlbumDetailView.as_view(), name='album_detail'),
    path('albums/create/', AlbumCreateView.as_view(), name='album_create'),
    path('albums/<int:pk>/update/', AlbumUpdateView.as_view(), name='album_update'),
    path('albums/<int:pk>/delete/', AlbumDeleteView.as_view(), name='album_delete'),
    
    path('admin/albums/<int:pk>/upload-images/', AlbumUploadImagesView.as_view(), name='album_upload_images'),
    # 图集图片排序接口（Django Admin 后台拖拽排序使用）
    path('admin/albums/<int:pk>/reorder-images/', AlbumReorderImagesView.as_view(), name='album_reorder_images'),

    path('notes/', NoteListView.as_view(), name='note_list'),
    path('notes/<int:pk>/', NoteDetailView.as_view(), name='note_detail'),

    path('admin/comics/create/', ComicCreateView.as_view(), name='comic_create'),
    path('admin/comics/<slug:slug>/update/', ComicUpdateView.as_view(), name='comic_update'),
    path('admin/comics/<slug:slug>/delete/', ComicDeleteView.as_view(), name='comic_delete'),
    # 漫画封面上传接口（Django Admin 后台使用）
    path('admin/comics/<int:pk>/upload-cover/', ComicUploadCoverView.as_view(), name='comic_upload_cover'),
    # 漫画章节图片批量上传接口（Django Admin 后台使用）
    path('admin/comic-chapters/<int:pk>/upload-images/', ComicChapterUploadImagesView.as_view(), name='comic_chapter_upload_images'),
    # 漫画章节图片排序接口（Django Admin 后台拖拽排序使用）
    path('admin/comic-chapters/<int:pk>/reorder-images/', ComicChapterReorderImagesView.as_view(), name='comic_chapter_reorder_images'),

    path('admin/comic-chapters/create/', ComicChapterCreateView.as_view(), name='comic_chapter_create'),
    path('admin/comic-chapters/<int:pk>/update/', ComicChapterUpdateView.as_view(), name='comic_chapter_update'),
    path('admin/comic-chapters/<int:pk>/delete/', ComicChapterDeleteView.as_view(), name='comic_chapter_delete'),

    path('admin/videos/create/', VideoCreateView.as_view(), name='video_create'),
    path('admin/videos/<slug:slug>/update/', VideoUpdateView.as_view(), name='video_update'),
    path('admin/videos/<slug:slug>/delete/', VideoDeleteView.as_view(), name='video_delete'),

    path('admin/categories/create/', CategoryCreateView.as_view(), name='category_create'),
    path('admin/categories/<slug:slug>/update/', CategoryUpdateView.as_view(), name='category_update'),
    path('admin/categories/<slug:slug>/delete/', CategoryDeleteView.as_view(), name='category_delete'),

    path('upload/image/', ImageUploadView.as_view(), name='image_upload'),
    path('upload/video/', VideoUploadView.as_view(), name='video_upload'),
    path('upload/video-folder/', VideoFolderUploadView.as_view(), name='video_folder_upload'),
    path('upload/video-thumbnail/', VideoThumbnailUploadView.as_view(), name='video_thumbnail_upload'),

    path('sync-media/', sync_media_view, name='sync_media'),
    path('cleanup-temp/', cleanup_temp_files, name='cleanup_temp'),
    
    path('users/', UserListView.as_view(), name='user_list'),
    path('users/<int:pk>/', UserUpdateView.as_view(), name='user_update'),
    path('change-password/', change_password_view, name='change_password'),
    path('admin/users/<int:user_id>/reset-password/', UserPasswordResetView.as_view(), name='user_password_reset'),

    path('cloud/', CloudFileListView.as_view(), name='cloud_list'),
    path('cloud/<int:id>/', CloudFileDetailView.as_view(), name='cloud_detail'),
    path('cloud/create-folder/', create_folder_view, name='cloud_create_folder'),
    
    path('users/me/', UserProfileView.as_view(), name='user_profile'),
    path('users/me/stats/', UserStatsView.as_view(), name='user_stats'),
    path('users/me/elevate/', UserElevationView.as_view(), name='user_elevate'),

    # AI 对话接口
    path('chat/', chat, name='chat'),
    path('chat/stream/', chat_stream, name='chat_stream'),
]