from rest_framework import generics, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from django.contrib.auth.models import User
from django.contrib.auth import authenticate
from django.conf import settings
import os
import re
import uuid
from .models import Category, Comic, ComicChapter, Video, Profile, Note, CloudFile
from .serializers import (
    UserSerializer, CategorySerializer,
    ComicListSerializer, ComicDetailSerializer, ComicChapterSerializer, ComicCreateUpdateSerializer,
    VideoListSerializer, VideoDetailSerializer, VideoCreateUpdateSerializer,
    NoteSerializer, CloudFileSerializer
)

# 自定义权限类
class IsAdminPermission(IsAuthenticated):
    def has_permission(self, request, view):
        user = request.user
        if hasattr(user, 'profile') and user.profile.user_type == 'admin':
            return True
        return False

class RegisterView(generics.CreateAPIView):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [AllowAny]

class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        username = request.data.get('username')
        password = request.data.get('password')

        if not username:
            return Response({'error': '请输入用户名', 'detail': '用户名不能为空'}, status=status.HTTP_400_BAD_REQUEST)

        if not password:
            return Response({'error': '请输入密码', 'detail': '密码不能为空'}, status=status.HTTP_400_BAD_REQUEST)

        user = authenticate(username=username, password=password)

        if user is None:
            return Response({
                'error': '用户名或密码错误',
                'detail': f'无法验证用户"{username}"的凭据，请检查用户名和密码是否正确'
            }, status=status.HTTP_401_UNAUTHORIZED)

        if not user.is_active:
            return Response({
                'error': '账户已被禁用',
                'detail': f'用户"{username}"的账户已被禁用，请联系管理员'
            }, status=status.HTTP_401_UNAUTHORIZED)

        from rest_framework_simplejwt.tokens import RefreshToken
        refresh = RefreshToken.for_user(user)
        user_type = 'user'
        if hasattr(user, 'profile'):
            user_type = user.profile.user_type
        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'user': {
                'id': user.id,
                'username': user.username,
                'email': user.email,
                'user_type': user_type
            }
        })

class ComicListView(generics.ListAPIView):
    serializer_class = ComicListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Comic.objects.all().order_by('-created_at')
        category_slug = self.request.query_params.get('category', None)
        
        if not (hasattr(self.request.user, 'profile') and self.request.user.profile.user_type == 'admin'):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
        
        if category_slug:
            queryset = queryset.filter(categories__slug=category_slug)
        
        return queryset

class ComicDetailView(generics.RetrieveAPIView):
    serializer_class = ComicDetailSerializer
    lookup_field = 'slug'
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Comic.objects.all()
        
        if not (hasattr(self.request.user, 'profile') and self.request.user.profile.user_type == 'admin'):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
        
        return queryset

class VideoListView(generics.ListAPIView):
    serializer_class = VideoListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Video.objects.all().order_by('-created_at')
        category_slug = self.request.query_params.get('category', None)
        
        if not (hasattr(self.request.user, 'profile') and self.request.user.profile.user_type == 'admin'):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
        
        if category_slug:
            queryset = queryset.filter(categories__slug=category_slug)
        
        return queryset

class VideoDetailView(generics.RetrieveAPIView):
    serializer_class = VideoDetailSerializer
    lookup_field = 'slug'
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Video.objects.all()
        
        if not (hasattr(self.request.user, 'profile') and self.request.user.profile.user_type == 'admin'):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
        
        return queryset

# 管理相关接口

# 漫画管理
class ComicCreateView(generics.CreateAPIView):
    queryset = Comic.objects.all()
    serializer_class = ComicCreateUpdateSerializer
    permission_classes = [IsAdminPermission]

class ComicUpdateView(generics.UpdateAPIView):
    queryset = Comic.objects.all()
    serializer_class = ComicCreateUpdateSerializer
    lookup_field = 'slug'
    permission_classes = [IsAdminPermission]

class ComicDeleteView(generics.DestroyAPIView):
    queryset = Comic.objects.all()
    lookup_field = 'slug'
    permission_classes = [IsAdminPermission]

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()
        # 删除漫画对应的文件目录
        comic_dir = os.path.join(settings.MEDIA_ROOT, 'comics', instance.slug)
        if os.path.exists(comic_dir):
            import shutil
            shutil.rmtree(comic_dir)
        return super().destroy(request, *args, **kwargs)

# 漫画章节管理
class ComicChapterCreateView(generics.CreateAPIView):
    queryset = ComicChapter.objects.all()
    serializer_class = ComicChapterSerializer
    permission_classes = [IsAdminPermission]

class ComicChapterUpdateView(generics.UpdateAPIView):
    queryset = ComicChapter.objects.all()
    serializer_class = ComicChapterSerializer
    permission_classes = [IsAdminPermission]

class ComicChapterDeleteView(generics.DestroyAPIView):
    queryset = ComicChapter.objects.all()
    permission_classes = [IsAdminPermission]

# 视频管理
class VideoCreateView(generics.CreateAPIView):
    queryset = Video.objects.all()
    serializer_class = VideoCreateUpdateSerializer
    permission_classes = [IsAdminPermission]

class VideoUpdateView(generics.UpdateAPIView):
    queryset = Video.objects.all()
    serializer_class = VideoCreateUpdateSerializer
    lookup_field = 'slug'
    permission_classes = [IsAdminPermission]

class VideoDeleteView(generics.DestroyAPIView):
    queryset = Video.objects.all()
    lookup_field = 'slug'
    permission_classes = [IsAdminPermission]

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()
        # 删除视频缩略图文件
        if instance.thumbnail:
            try:
                thumb_path = instance.thumbnail.path
                if os.path.exists(thumb_path):
                    os.remove(thumb_path)
            except Exception as e:
                print(f"删除缩略图失败: {e}")
        # 删除视频文件
        if instance.video_file:
            try:
                video_path = instance.video_file.path
                if os.path.exists(video_path):
                    os.remove(video_path)
            except Exception as e:
                print(f"删除视频文件失败: {e}")
        return super().destroy(request, *args, **kwargs)

# 分类管理
class CategoryCreateView(generics.CreateAPIView):
    queryset = Category.objects.all()
    serializer_class = CategorySerializer
    permission_classes = [IsAdminPermission]

class CategoryUpdateView(generics.UpdateAPIView):
    queryset = Category.objects.all()
    serializer_class = CategorySerializer
    lookup_field = 'slug'
    permission_classes = [IsAdminPermission]

class CategoryDeleteView(generics.DestroyAPIView):
    queryset = Category.objects.all()
    lookup_field = 'slug'
    permission_classes = [IsAdminPermission]

# 用户密码重置
class UserPasswordResetView(APIView):
    permission_classes = [IsAdminPermission]

    def post(self, request, user_id):
        new_password = request.data.get('new_password')

        if not new_password:
            return Response({'error': '请提供新密码'}, status=status.HTTP_400_BAD_REQUEST)

        if len(new_password) < 6:
            return Response({'error': '密码长度不能少于6个字符'}, status=status.HTTP_400_BAD_REQUEST)

        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': f'找不到ID为{user_id}的用户'}, status=status.HTTP_404_NOT_FOUND)

        user.set_password(new_password)
        user.save()

        return Response({'message': f'用户{user.username}的密码已成功重置'})

# 图片上传视图
class ImageUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        file = request.FILES.get('file')
        if not file:
            return Response({'error': '没有上传文件'}, status=status.HTTP_400_BAD_REQUEST)

        # 允许的图片格式
        allowed_types = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
        if file.content_type not in allowed_types:
            return Response({'error': '不支持的图片格式'}, status=status.HTTP_400_BAD_REQUEST)

        # 检查文件大小（限制10MB）
        if file.size > 10 * 1024 * 1024:
            return Response({'error': '文件大小不能超过10MB'}, status=status.HTTP_400_BAD_REQUEST)

        # 检查文件扩展名
        ext = os.path.splitext(file.name)[1].lower()

        # 获取漫画别名（如果提供）
        comic_slug = request.data.get('comic_slug')
        chapter_index = request.data.get('chapter_index', 0)
        is_cover = request.data.get('is_cover', False)

        if comic_slug:
            # 为漫画创建目录结构
            comic_dir = os.path.join(settings.MEDIA_ROOT, 'comics', comic_slug)
            if not os.path.exists(comic_dir):
                os.makedirs(comic_dir)

            # 确定文件名
            if is_cover:
                # 封面图片命名为 000
                filename = f"000{ext}"
                filepath = os.path.join(comic_dir, filename)
                relative_path = f"comics/{comic_slug}/{filename}"
            else:
                # 章节图片从 001 开始
                chapter_dir = os.path.join(comic_dir, f"chapter_{chapter_index}")
                if not os.path.exists(chapter_dir):
                    os.makedirs(chapter_dir)

                # 找到当前最大的序号
                existing_files = [f for f in os.listdir(chapter_dir) if f.endswith(tuple(['.jpg', '.jpeg', '.png', '.gif', '.webp']))]
                max_idx = 0
                for f in existing_files:
                    try:
                        idx = int(os.path.splitext(f)[0])
                        if idx > max_idx:
                            max_idx = idx
                    except:
                        pass

                # 新文件名
                new_idx = max_idx + 1
                filename = f"{new_idx:03d}{ext}"
                filepath = os.path.join(chapter_dir, filename)
                relative_path = f"comics/{comic_slug}/chapter_{chapter_index}/{filename}"
        else:
            # 如果没有提供漫画别名，使用原有的上传逻辑
            # 生成唯一文件名
            filename = f"{uuid.uuid4().hex}{ext}"

            # 确保上传目录存在
            upload_dir = os.path.join(settings.MEDIA_ROOT, 'uploads')
            if not os.path.exists(upload_dir):
                os.makedirs(upload_dir)

            # 保存文件
            filepath = os.path.join(upload_dir, filename)
            relative_path = f"uploads/{filename}"

        # 保存文件
        with open(filepath, 'wb+') as destination:
            for chunk in file.chunks():
                destination.write(chunk)

        # 返回相对路径
        return Response({'path': relative_path}, status=status.HTTP_201_CREATED)


# 视频上传视图
class VideoUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        file = request.FILES.get('file')
        if not file:
            return Response({'error': '没有上传文件', 'detail': 'FILES中不存在file字段'}, status=status.HTTP_400_BAD_REQUEST)

        allowed_types = ['video/mp4', 'video/avi', 'video/mov', 'video/mkv', 'video/webm']
        if file.content_type not in allowed_types:
            return Response({'error': '不支持的视频格式', 'detail': f'当前格式: {file.content_type}, 支持的格式: {allowed_types}'}, status=status.HTTP_400_BAD_REQUEST)

        ext = os.path.splitext(file.name)[1].lower()

        videos_dir = os.path.join(settings.MEDIA_ROOT, 'videos')
        if not os.path.exists(videos_dir):
            os.makedirs(videos_dir)

        filename = f"{uuid.uuid4().hex}{ext}"
        filepath = os.path.join(videos_dir, filename)
        relative_path = f"videos/{filename}"

        try:
            with open(filepath, 'wb+') as destination:
                for chunk in file.chunks():
                    destination.write(chunk)
        except Exception as e:
            return Response({'error': '保存文件失败', 'detail': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        thumbnail_filename = f"{uuid.uuid4().hex}.jpg"
        thumbnail_path = os.path.join(videos_dir, thumbnail_filename)
        thumbnail_relative_path = f"videos/{thumbnail_filename}"

        try:
            import subprocess
            result = subprocess.run(
                ['ffmpeg', '-i', filepath, '-ss', '00:00:01', '-vframes', '1', '-q:v', '2', thumbnail_path, '-y'],
                capture_output=True,
                text=True,
                timeout=30
            )
            if result.returncode == 0 and os.path.exists(thumbnail_path):
                return Response({'path': relative_path, 'thumbnail': thumbnail_relative_path}, status=status.HTTP_201_CREATED)
            else:
                return Response({'path': relative_path}, status=status.HTTP_201_CREATED)
        except Exception as e:
            print(f"生成视频缩略图失败: {e}")
            return Response({'path': relative_path}, status=status.HTTP_201_CREATED)


# 视频文件夹上传视图（支持m3u8）
class VideoFolderUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        files = request.FILES.getlist('files')
        if not files or len(files) == 0:
            return Response({'error': '没有上传文件', 'detail': 'FILES中不存在files字段'}, status=status.HTTP_400_BAD_REQUEST)

        # 查找m3u8文件
        m3u8_file = None
        ts_files = []
        key_files = []
        other_files = []
        
        for file in files:
            filename = file.name.lower()
            if filename.endswith('.m3u8'):
                m3u8_file = file
            elif filename.endswith('.ts'):
                ts_files.append(file)
            elif filename.endswith('.key'):
                key_files.append(file)
            else:
                other_files.append(file)
        
        if not m3u8_file:
            return Response({'error': '没有找到m3u8文件', 'detail': '请确保上传的文件夹中包含index.m3u8文件'}, status=status.HTTP_400_BAD_REQUEST)

        # 创建视频目录
        video_uuid = uuid.uuid4().hex
        video_dir = os.path.join(settings.MEDIA_ROOT, 'videos', video_uuid)
        if not os.path.exists(video_dir):
            os.makedirs(video_dir)

        # 保存所有文件
        m3u8_path = None
        for file in files:
            # 获取文件名（处理完整路径的情况）
            filename = os.path.basename(file.name)
            filepath = os.path.join(video_dir, filename)
            relative_path = f"videos/{video_uuid}/{filename}"
            
            if filename.lower().endswith('.m3u8'):
                m3u8_path = relative_path
            
            try:
                with open(filepath, 'wb+') as destination:
                    for chunk in file.chunks():
                        destination.write(chunk)
            except Exception as e:
                # 清理已上传的文件
                import shutil
                if os.path.exists(video_dir):
                    shutil.rmtree(video_dir)
                return Response({'error': '保存文件失败', 'detail': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        # 生成缩略图（尝试从第一个ts文件提取）
        thumbnail_path = None
        if ts_files:
            first_ts = ts_files[0]
            ts_filename = os.path.basename(first_ts.name)
            ts_filepath = os.path.join(video_dir, ts_filename)
            
            thumbnail_filename = f"{video_uuid}.jpg"
            thumbnail_filepath = os.path.join(settings.MEDIA_ROOT, 'videos', thumbnail_filename)
            
            try:
                import subprocess
                result = subprocess.run(
                    ['ffmpeg', '-i', ts_filepath, '-ss', '00:00:00', '-vframes', '1', '-q:v', '2', thumbnail_filepath, '-y'],
                    capture_output=True,
                    text=True,
                    timeout=30
                )
                if result.returncode == 0 and os.path.exists(thumbnail_filepath):
                    thumbnail_path = f"videos/{thumbnail_filename}"
            except Exception as e:
                print(f"生成视频缩略图失败: {e}")

        return Response({
            'm3u8_path': m3u8_path,
            'thumbnail': thumbnail_path,
            'message': 'm3u8视频文件夹上传成功'
        }, status=status.HTTP_201_CREATED)


# 视频缩略图上传视图
class VideoThumbnailUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        file = request.FILES.get('file')
        if not file:
            return Response({'error': '没有上传文件'}, status=status.HTTP_400_BAD_REQUEST)

        # 允许的图片格式
        allowed_types = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
        if file.content_type not in allowed_types:
            return Response({'error': '不支持的图片格式'}, status=status.HTTP_400_BAD_REQUEST)

        # 检查文件大小（限制5MB）
        if file.size > 5 * 1024 * 1024:
            return Response({'error': '文件大小不能超过5MB'}, status=status.HTTP_400_BAD_REQUEST)

        # 检查文件扩展名
        ext = os.path.splitext(file.name)[1].lower()

        # 保存到视频目录
        videos_dir = os.path.join(settings.MEDIA_ROOT, 'videos')
        if not os.path.exists(videos_dir):
            os.makedirs(videos_dir)

        # 生成唯一文件名
        filename = f"{uuid.uuid4().hex}{ext}"
        filepath = os.path.join(videos_dir, filename)
        relative_path = f"videos/{filename}"

        # 保存文件
        with open(filepath, 'wb+') as destination:
            for chunk in file.chunks():
                destination.write(chunk)

        return Response({'path': relative_path}, status=status.HTTP_201_CREATED)


# 同步媒体文件
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def sync_media_view(request):
    """同步媒体文件到数据库"""
    try:
        synced_comics = 0
        synced_videos = 0
        comics_dir = os.path.join(settings.MEDIA_ROOT, 'comics')
        videos_dir = os.path.join(settings.MEDIA_ROOT, 'videos')
        
        # 同步漫画
        if os.path.exists(comics_dir):
            for comic_slug in os.listdir(comics_dir):
                comic_path = os.path.join(comics_dir, comic_slug)
                if not os.path.isdir(comic_path):
                    continue
                
                comic, created = Comic.objects.get_or_create(
                    slug=comic_slug,
                    defaults={
                        'title': comic_slug,
                        'description': '同步创建的漫画',
                        'author': '未知'
                    }
                )
                
                if created:
                    synced_comics += 1
                
                # 收集所有散落的图片文件（不在chapter_N目录中的）
                loose_images = []
                chapter_dirs = []
                
                for item in os.listdir(comic_path):
                    item_path = os.path.join(comic_path, item)
                    if os.path.isdir(item_path):
                        chapter_match = re.match(r'chapter_(\d+)', item)
                        if chapter_match:
                            chapter_dirs.append((item, int(chapter_match.group(1)), item_path))
                    elif item.lower().endswith(('.jpg', '.jpeg', '.png', '.gif', '.webp')):
                        # 散落的图片文件可能是封面
                        loose_images.append(f'comics/{comic_slug}/{item}')
                
                # 处理章节目录
                for chapter_dir, chapter_index, chapter_path in sorted(chapter_dirs, key=lambda x: x[1]):
                    chapter, chapter_created = ComicChapter.objects.get_or_create(
                        comic=comic,
                        chapter_number=chapter_index,
                        defaults={'title': f'第{chapter_index}章'}
                    )
                    
                    if chapter_created:
                        # 收集章节图片
                        images = []
                        for img_file in sorted(os.listdir(chapter_path)):
                            if img_file.lower().endswith(('.jpg', '.jpeg', '.png', '.gif', '.webp')):
                                img_relative = f'comics/{comic_slug}/{chapter_dir}/{img_file}'
                                images.append(img_relative)
                        
                        if images:
                            chapter.images = images
                            chapter.save()
                        
                        # 如果漫画没有封面，使用第一章的第一张图片
                        if not comic.cover_image and images:
                            comic.cover_image = images[0]
                            comic.save()
                
                # 检查是否有000封面文件
                for ext in ['.jpg', '.jpeg', '.png', '.gif', '.webp']:
                    cover_path = os.path.join(comic_path, f'000{ext}')
                    if os.path.exists(cover_path):
                        comic.cover_image = f'comics/{comic_slug}/000{ext}'
                        comic.save()
                        break
                
                # 如果漫画仍然没有封面，使用散落的图片
                if not comic.cover_image and loose_images:
                    comic.cover_image = loose_images[0]
                    comic.save()
        
        # 同步视频
        if os.path.exists(videos_dir):
            for video_file in os.listdir(videos_dir):
                video_path = os.path.join(videos_dir, video_file)
                if os.path.isfile(video_path):
                    # 检查是否是视频文件
                    if video_file.lower().endswith(('.mp4', '.avi', '.mov', '.mkv', '.webm')):
                        video_slug = os.path.splitext(video_file)[0]
                        
                        video, created = Video.objects.get_or_create(
                            slug=video_slug,
                            defaults={
                                'title': video_slug,
                                'description': '同步创建的视频',
                                'duration': 0
                            }
                        )
                        
                        if created:
                            synced_videos += 1
                        
                        # 设置视频文件路径
                        if not video.video_file:
                            video.video_file = f'videos/{video_file}'
                            video.save()
                        
                        # 查找或生成缩略图
                        thumbnail_name = os.path.splitext(video_file)[0] + '.jpg'
                        thumbnail_path = os.path.join(videos_dir, thumbnail_name)
                        
                        if not os.path.exists(thumbnail_path):
                            # 使用ffmpeg从视频中抽帧生成缩略图
                            try:
                                import subprocess
                                result = subprocess.run(
                                    ['ffmpeg', '-i', video_path, '-ss', '00:00:01', '-vframes', '1', '-q:v', '2', thumbnail_path, '-y'],
                                    capture_output=True,
                                    text=True,
                                    timeout=30
                                )
                                if result.returncode == 0 and os.path.exists(thumbnail_path):
                                    video.thumbnail = f'videos/{thumbnail_name}'
                                    video.save()
                            except Exception as thumb_err:
                                print(f"生成视频缩略图失败: {thumb_err}")
                        else:
                            video.thumbnail = f'videos/{thumbnail_name}'
                            video.save()
        
        return Response({
            'message': f'媒体文件同步成功',
            'synced_comics': synced_comics,
            'synced_videos': synced_videos
        }, status=status.HTTP_200_OK)
    except Exception as e:
        import traceback
        error_details = traceback.format_exc()
        print(f"同步媒体文件错误: {error_details}")
        return Response({'error': str(e), 'details': error_details[:500]}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

# 清理临时文件
@api_view(['POST'])
@permission_classes([IsAdminPermission])
def cleanup_temp_files(request):
    """清理临时文件"""
    try:
        comic_slug = request.data.get('comic_slug')
        if comic_slug:
            # 清理漫画临时目录
            comic_dir = os.path.join(settings.MEDIA_ROOT, 'comics', comic_slug)
            if os.path.exists(comic_dir):
                import shutil
                shutil.rmtree(comic_dir)
                return Response({'message': '临时文件清理成功'}, status=status.HTTP_200_OK)
        return Response({'message': '没有需要清理的临时文件'}, status=status.HTTP_200_OK)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

# 分类管理
class CategoryListView(generics.ListCreateAPIView):
    serializer_class = CategorySerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        # 管理员可以看到所有分类
        if hasattr(self.request.user, 'profile') and self.request.user.profile.user_type == 'admin':
            return Category.objects.all()
        # 普通用户只能看到普通权限的分类
        return Category.objects.filter(permission_level='regular')

class CategoryDetailView(generics.RetrieveUpdateDestroyAPIView):
    serializer_class = CategorySerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        # 管理员可以看到所有分类
        if hasattr(self.request.user, 'profile') and self.request.user.profile.user_type == 'admin':
            return Category.objects.all()
        # 普通用户只能看到普通权限的分类
        return Category.objects.filter(permission_level='regular')

# 笔记管理
class NoteListView(generics.ListCreateAPIView):
    serializer_class = NoteSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Note.objects.filter(user=self.request.user).order_by('-updated_at')

class NoteDetailView(generics.RetrieveUpdateDestroyAPIView):
    serializer_class = NoteSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Note.objects.filter(user=self.request.user)

# 用户管理相关接口
class UserListView(generics.ListAPIView):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [IsAdminPermission]

class UserUpdateView(generics.UpdateAPIView):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [IsAdminPermission]
    http_method_names = ['put', 'patch', 'options']

# 修改密码
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def change_password_view(request):
    current_password = request.data.get('current_password')
    new_password = request.data.get('new_password')
    
    if not current_password or not new_password:
        return Response({'error': '缺少必要参数'}, status=status.HTTP_400_BAD_REQUEST)
    
    user = request.user
    if not user.check_password(current_password):
        return Response({'error': '当前密码错误'}, status=status.HTTP_400_BAD_REQUEST)
    
    user.set_password(new_password)
    user.save()
    return Response({'message': '密码修改成功'}, status=status.HTTP_200_OK)

# 云盘视图
class CloudFileListView(generics.ListCreateAPIView):
    serializer_class = CloudFileSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = CloudFile.objects.filter(user=self.request.user)
        parent_id = self.request.query_params.get('parent', None)
        if parent_id:
            queryset = queryset.filter(parent_id=parent_id)
        else:
            queryset = queryset.filter(parent__isnull=True)
        return queryset

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

class CloudFileDetailView(generics.RetrieveUpdateDestroyAPIView):
    serializer_class = CloudFileSerializer
    permission_classes = [IsAuthenticated]
    lookup_field = 'id'

    def get_queryset(self):
        return CloudFile.objects.filter(user=self.request.user)

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()
        
        # 递归删除所有子文件和文件夹
        def delete_recursive(file_obj):
            # 先收集所有子对象
            children = list(file_obj.children.all())
            # 递归删除子对象
            for child in children:
                delete_recursive(child)
            # 删除物理文件
            if file_obj.file_path and file_obj.file_type == 'file':
                try:
                    file_path = file_obj.file_path.path
                    if os.path.exists(file_path):
                        os.remove(file_path)
                except Exception as e:
                    print(f"删除文件失败: {e}")
            # 删除数据库记录
            file_obj.delete()
        
        delete_recursive(instance)
        return Response(status=status.HTTP_204_NO_CONTENT)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def create_folder_view(request):
    name = request.data.get('name')
    parent_id = request.data.get('parent', None)

    if not name:
        return Response({'error': '文件夹名称不能为空'}, status=status.HTTP_400_BAD_REQUEST)

    parent = None
    if parent_id:
        try:
            parent = CloudFile.objects.get(id=parent_id, user=request.user)
        except CloudFile.DoesNotExist:
            return Response({'error': '父文件夹不存在'}, status=status.HTTP_404_NOT_FOUND)

    folder = CloudFile.objects.create(
        name=name,
        file_type='folder',
        parent=parent,
        user=request.user
    )
    serializer = CloudFileSerializer(folder, context={'request': request})
    return Response(serializer.data, status=status.HTTP_201_CREATED)
