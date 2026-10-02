from rest_framework import generics, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from django.contrib.auth.models import User
from django.contrib.auth import authenticate
from django.conf import settings
from django.db import models
import os
import re
import uuid
import logging
from .models import Category, Comic, ComicChapter, Video, Profile, Note, CloudFile, Image, Album, AlbumImage
from .serializers import (
    UserSerializer, CategorySerializer,
    ComicListSerializer, ComicDetailSerializer, ComicChapterSerializer, ComicCreateUpdateSerializer,
    VideoListSerializer, VideoDetailSerializer, VideoCreateUpdateSerializer,
    NoteSerializer, CloudFileSerializer,
    ImageSerializer, AlbumSerializer
)

logger = logging.getLogger(__name__)


def is_effective_admin(request):
    """
    判断请求用户是否具有管理员权限（考虑隐私模式）

    当请求头中携带 X-Privacy-Mode: true 时，即使该用户本身是 admin，
    也按普通用户处理（不返回特殊权限内容），用于支持 APK 端隐私开关

    :param request: HTTP 请求对象
    :return: True 表示当前请求具有管理员权限
    """
    # 隐私模式开启时一律视为普通用户
    if request.META.get('HTTP_X_PRIVACY_MODE', '').lower() == 'true':
        return False
    user = request.user
    return hasattr(user, 'profile') and user.profile.user_type == 'admin'


class IsAdminPermission(IsAuthenticated):
    def has_permission(self, request, view):
        # 注意：真正的管理员操作接口（增删改）不受隐私模式影响
        # 隐私模式只影响列表/详情查询的过滤逻辑
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
        
        if not is_effective_admin(self.request):
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

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()

        return queryset

class VideoListView(generics.ListAPIView):
    serializer_class = VideoListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Video.objects.all().order_by('-created_at')
        category_slug = self.request.query_params.get('category', None)
        video_type = self.request.query_params.get('type', None)

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
            # 非管理员（含隐私模式）：特辑（video_type='special'）一律视为特殊内容，全部过滤
            queryset = queryset.exclude(video_type='special')

        if category_slug:
            queryset = queryset.filter(categories__slug=category_slug)

        if video_type:
            queryset = queryset.filter(video_type=video_type)

        return queryset

class VideoDetailView(generics.RetrieveAPIView):
    serializer_class = VideoDetailSerializer
    lookup_field = 'slug'
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Video.objects.all()

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
            # 非管理员（含隐私模式）：特辑一律视为特殊内容，全部过滤
            queryset = queryset.exclude(video_type='special')

        return queryset

class ImageListView(generics.ListAPIView):
    serializer_class = ImageSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        # 图片分类与图集物理隔离：
        # - 单张图片 resource_type='image'，存于 images/ 目录，只它属于“图片”分类
        # - 图集图片 album_image / 图集封面 album_cover 存于 albums/ 目录，
        #   仅通过图集接口(/api/albums/)访问，不允许出现在图片分类列表中
        queryset = Image.objects.all().order_by('-created_at')
        category_slug = self.request.query_params.get('category', None)
        resource_type = self.request.query_params.get('type', None)

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()

        if category_slug:
            queryset = queryset.filter(categories__slug=category_slug)

        # 资源类型过滤规则：
        # - type=all：仅管理员可用（后台“图片管理”需看到图集图片/封面）；非管理员降级为仅单图
        # - type=image / album_image / album_cover：按指定类型精确返回
        # - 不传 type 或传其他值：默认只返回独立单张图片，从根源上与图集隔离
        valid_types = dict(Image.RESOURCE_TYPE_CHOICES)
        if resource_type == 'all':
            if not is_effective_admin(self.request):
                queryset = queryset.filter(resource_type='image')
        elif resource_type in valid_types:
            queryset = queryset.filter(resource_type=resource_type)
        else:
            queryset = queryset.filter(resource_type='image')

        return queryset

class ImageDetailView(generics.RetrieveAPIView):
    serializer_class = ImageSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Image.objects.all()

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()

        return queryset

class ImageCreateView(generics.CreateAPIView):
    serializer_class = ImageSerializer
    permission_classes = [IsAuthenticated]

class ImageUpdateView(generics.UpdateAPIView):
    serializer_class = ImageSerializer
    permission_classes = [IsAuthenticated]

class ImageDeleteView(generics.DestroyAPIView):
    serializer_class = ImageSerializer
    permission_classes = [IsAuthenticated]


class ImageBatchUploadView(APIView):
    """图片批量上传视图 - 支持多文件 + 标签分配"""
    permission_classes = [IsAuthenticated]

    # 允许的图片 MIME 类型
    ALLOWED_CONTENT_TYPES = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif',
        'image/webp', 'image/bmp', 'image/tiff',
    ]
    # 允许的文件扩展名
    ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.svg']

    def post(self, request):
        """处理批量图片上传"""
        files = request.FILES.getlist('images')
        category_ids = request.data.getlist('categories', [])
        title_prefix = request.data.get('title_prefix', '图片')

        if not files:
            return Response({'error': '请至少上传一张图片'}, status=status.HTTP_400_BAD_REQUEST)

        logger.info(f"批量上传图片: {len(files)} 张, 标签: {category_ids}")

        # 解析标签
        categories = []
        if category_ids:
            try:
                categories = list(Category.objects.filter(id__in=category_ids))
            except Exception as e:
                logger.warning(f"解析标签失败: {str(e)}")

        upload_dir = os.path.join(settings.MEDIA_ROOT, 'images')
        if not os.path.exists(upload_dir):
            os.makedirs(upload_dir)

        uploaded_count = 0
        skipped_files = []

        for idx, file in enumerate(files):
            try:
                # 验证文件类型
                content_type = file.content_type or ''
                ext = os.path.splitext(file.name)[1].lower()

                if content_type not in self.ALLOWED_CONTENT_TYPES and ext not in self.ALLOWED_EXTENSIONS:
                    skipped_files.append(f"{file.name}({content_type})")
                    continue

                resource_id = str(uuid.uuid4())
                filename = f"{resource_id}{ext}"
                relative_path = f"images/{filename}"
                filepath = os.path.join(upload_dir, filename)

                # 保存文件
                with open(filepath, 'wb+') as destination:
                    for chunk in file.chunks():
                        destination.write(chunk)

                # 创建 Image 记录
                image = Image.objects.create(
                    resource_id=resource_id,
                    image_file=relative_path,
                    title=f"{title_prefix}{idx + 1}",
                    resource_type='image',
                    uploader=request.user
                )

                # 分配标签
                if categories:
                    image.categories.set(categories)

                uploaded_count += 1
                logger.debug(f"已保存: {file.name} -> {filename}")

            except Exception as e:
                logger.error(f"处理文件 {file.name} 时出错: {str(e)}")
                skipped_files.append(f"{file.name}(错误: {str(e)})")
                continue

        result_data = {
            'success': True,
            'uploaded_count': uploaded_count,
            'total_files': len(files)
        }
        if skipped_files:
            result_data['skipped_files'] = skipped_files
            result_data['warning'] = f"成功 {uploaded_count} 张，跳过 {len(skipped_files)} 张"

        logger.info(f"批量上传完成: 成功 {uploaded_count}/{len(files)}")
        return Response(result_data, status=status.HTTP_201_CREATED)


class AlbumListView(generics.ListAPIView):
    serializer_class = AlbumSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Album.objects.all().order_by('-created_at')
        category_slug = self.request.query_params.get('category', None)

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()

        if category_slug:
            queryset = queryset.filter(categories__slug=category_slug)

        return queryset

class AlbumDetailView(generics.RetrieveAPIView):
    serializer_class = AlbumSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        queryset = Album.objects.all()

        if not is_effective_admin(self.request):
            special_categories = Category.objects.filter(permission_level='special')
            queryset = queryset.exclude(categories__in=special_categories).distinct()
        
        return queryset

class AlbumCreateView(APIView):
    """图集创建视图 - 按上传顺序保存图片"""
    permission_classes = [IsAuthenticated]

    # 允许的图片 MIME 类型
    ALLOWED_CONTENT_TYPES = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif', 
        'image/webp', 'image/bmp', 'image/tiff',
    ]
    # 允许的文件扩展名
    ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.svg']

    def post(self, request):
        title = request.data.get('title')
        description = request.data.get('description', '')
        author = request.data.get('author', '')
        files = request.FILES.getlist('files')
        category_ids = request.data.getlist('categories', [])

        if not title:
            return Response({'error': '标题不能为空'}, status=status.HTTP_400_BAD_REQUEST)

        if not files:
            return Response({'error': '请至少上传一张图片'}, status=status.HTTP_400_BAD_REQUEST)

        logger.info(f"创建图集: title={title}, 文件数={len(files)}, 用户={request.user.username}")

        album = Album.objects.create(
            title=title,
            description=description,
            author=author,
            uploader=request.user
        )

        if category_ids:
            try:
                categories = Category.objects.filter(id__in=category_ids)
                album.categories.set(categories)
            except Exception as e:
                logger.warning(f"设置分类失败: {str(e)}")

        album_dir = os.path.join(settings.MEDIA_ROOT, 'albums', album.resource_id)
        if not os.path.exists(album_dir):
            os.makedirs(album_dir)

        uploaded_images = []
        skipped_count = 0
        
        for idx, file in enumerate(files):
            try:
                # 验证文件类型（通过 MIME 或扩展名）
                content_type = file.content_type or ''
                ext = os.path.splitext(file.name)[1].lower()
                
                if content_type not in self.ALLOWED_CONTENT_TYPES and ext not in self.ALLOWED_EXTENSIONS:
                    skipped_count += 1
                    logger.warning(f"跳过不支持的文件: {file.name}, type={content_type}, ext={ext}")
                    continue

                resource_id = str(uuid.uuid4())

                if idx == 0:
                    filename = f"cover{ext}"
                    is_cover = True
                else:
                    filename = f"{resource_id}{ext}"
                    is_cover = False

                filepath = os.path.join(album_dir, filename)
                relative_path = f"albums/{album.resource_id}/{filename}"

                with open(filepath, 'wb+') as destination:
                    for chunk in file.chunks():
                        destination.write(chunk)

                image_resource_type = 'album_cover' if is_cover else 'album_image'
                image = Image.objects.create(
                    resource_id=resource_id,
                    image_file=relative_path,
                    title=f"图片{idx+1}",
                    resource_type=image_resource_type,
                    album_id=album.resource_id,
                    uploader=request.user
                )

                uploaded_images.append({
                    'id': image.id,
                    'resource_id': image.resource_id,
                    'path': request.build_absolute_uri(f'/media/{relative_path}'),
                    'is_cover': is_cover
                })

                if is_cover:
                    album.cover_image = image
                    album.save()
                else:
                    album.images.add(image)
                    AlbumImage.objects.create(
                        album=album,
                        image=image,
                        order=idx
                    )
                    
            except Exception as e:
                logger.error(f"处理文件 {file.name} 时出错: {str(e)}")
                skipped_count += 1
                continue

        logger.info(f"图集创建完成: 成功 {len(uploaded_images)}/{len(files)}, 跳过 {skipped_count}")
        
        response_data = {
            'id': album.id,
            'resource_id': album.resource_id,
            'title': album.title,
            'description': album.description,
            'author': album.author,
            'image_count': len(uploaded_images),
            'images': uploaded_images,
            'created_at': album.created_at
        }
        if skipped_count > 0:
            response_data['warning'] = f"成功 {len(uploaded_images)} 张，跳过 {skipped_count} 张"

        return Response(response_data, status=status.HTTP_201_CREATED)

class AlbumUpdateView(generics.UpdateAPIView):
    serializer_class = AlbumSerializer
    permission_classes = [IsAuthenticated]

class AlbumDeleteView(generics.DestroyAPIView):
    serializer_class = AlbumSerializer
    permission_classes = [IsAuthenticated]

class AlbumUploadImagesView(APIView):
    """图集图片上传视图 - 用于 Django Admin 后台直接上传图片"""
    permission_classes = [IsAuthenticated]
    
    # 允许的图片 MIME 类型
    ALLOWED_CONTENT_TYPES = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif', 
        'image/webp', 'image/bmp', 'image/tiff',
    ]
    # 允许的文件扩展名
    ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.svg']
    
    def post(self, request, pk):
        """处理图片上传请求（已修复并发竞态与顺序问题）"""
        try:
            # 获取图集
            try:
                album = Album.objects.get(pk=pk)
            except Album.DoesNotExist:
                logger.error(f"图集不存在: pk={pk}")
                return Response({'error': '图集不存在'}, status=status.HTTP_404_NOT_FOUND)

            # 获取上传的图片文件
            files = request.FILES.getlist('images')
            if not files:
                logger.warning("没有上传任何图片文件")
                return Response({'error': '没有上传任何图片'}, status=status.HTTP_400_BAD_REQUEST)

            logger.info(f"收到 {len(files)} 个文件上传请求，图集: {album.title}(id={album.id})")

            # 创建图集目录
            album_dir = os.path.join(settings.MEDIA_ROOT, 'albums', album.resource_id)
            if not os.path.exists(album_dir):
                os.makedirs(album_dir)

            # 先在事务外保存所有文件到磁盘（IO 不需要锁），并准备 AlbumImage 创建数据
            # 保持入参顺序，避免并发批次乱序
            pending_items = []  # 每项：(resource_id, relative_path, image_resource_type, image_title)
            skipped_files = []

            for idx, file in enumerate(files):
                try:
                    # 验证文件类型
                    content_type = file.content_type or ''
                    ext = os.path.splitext(file.name)[1].lower()

                    if content_type not in self.ALLOWED_CONTENT_TYPES and ext not in self.ALLOWED_EXTENSIONS:
                        skipped_files.append(f"{file.name}({content_type})")
                        logger.warning(f"跳过不支持的文件类型: {file.name}, type={content_type}, ext={ext}")
                        continue

                    resource_id = str(uuid.uuid4())
                    filename = f"{resource_id}{ext}"
                    filepath = os.path.join(album_dir, filename)
                    relative_path = f"albums/{album.resource_id}/{filename}"

                    # 保存文件
                    with open(filepath, 'wb+') as destination:
                        for chunk in file.chunks():
                            destination.write(chunk)

                    pending_items.append({
                        'resource_id': resource_id,
                        'relative_path': relative_path,
                        'ext': ext,
                        'original_name': file.name,
                    })
                    logger.debug(f"已暂存: {file.name} -> {filename}")

                except Exception as e:
                    logger.error(f"处理文件 {file.name} 时出错: {str(e)}")
                    skipped_files.append(f"{file.name}(错误: {str(e)})")
                    continue

            uploaded_count = len(pending_items)

            # 关键修复：使用事务 + select_for_update 锁定图集行
            # 避免并发批次读取相同的 last_order 并产生重复 order
            from django.db import transaction
            with transaction.atomic():
                # 锁定图集行，其他并发请求会在此等待
                locked_album = Album.objects.select_for_update().get(pk=album.pk)

                # 在锁内重新读取当前最大 order
                last_order = locked_album.album_images.aggregate(
                    max_order=models.Max('order')
                )['max_order'] or 0

                # 是否需要设置封面（在锁内判断，避免并发误判）
                need_cover = locked_album.cover_image is None

                # 按入参顺序逐张创建 Image 和 AlbumImage
                for idx, item in enumerate(pending_items):
                    order = last_order + idx + 1

                    # 第一张图作为封面（如果还没有封面）
                    is_first_as_cover = need_cover and idx == 0

                    if is_first_as_cover:
                        image_resource_type = 'album_cover'
                    else:
                        image_resource_type = 'album_image'

                    # 创建 Image 记录
                    image = Image.objects.create(
                        resource_id=item['resource_id'],
                        image_file=item['relative_path'],
                        title=f"图片{order}",
                        resource_type=image_resource_type,
                        album_id=locked_album.resource_id,
                        uploader=request.user
                    )

                    # 添加到 M2M 关系
                    locked_album.images.add(image)

                    # 保存排序
                    AlbumImage.objects.create(
                        album=locked_album,
                        image=image,
                        order=order
                    )

                    # 如果是封面，更新图集
                    if is_first_as_cover:
                        locked_album.cover_image = image

                # 一次性保存图集（仅 cover_image 可能变化）
                locked_album.save(update_fields=['cover_image'])

                final_total = locked_album.album_images.count()

            result_data = {
                'success': True,
                'uploaded_count': uploaded_count,
                'total_images': final_total
            }
            if skipped_files:
                result_data['skipped_files'] = skipped_files
                result_data['warning'] = f"成功 {uploaded_count} 张，跳过 {len(skipped_files)} 张"

            logger.info(f"上传完成: 成功 {uploaded_count}/{len(files)}, 跳过 {len(skipped_files)}, 总数 {final_total}")
            return Response(result_data)

        except Exception as e:
            logger.exception(f"上传图集图片时发生未预期的错误: {str(e)}")
            return Response({
                'error': str(e)
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

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
        comic_dir = os.path.join(settings.MEDIA_ROOT, 'comics', instance.slug)
        if os.path.exists(comic_dir):
            import shutil
            shutil.rmtree(comic_dir)
        return super().destroy(request, *args, **kwargs)

class ComicUploadCoverView(APIView):
    """漫画封面上传视图 - 用于 Django Admin 后台直接上传封面图片"""
    permission_classes = [IsAuthenticated]

    # 允许的图片 MIME 类型
    ALLOWED_CONTENT_TYPES = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif',
        'image/webp', 'image/bmp', 'image/tiff',
    ]
    # 允许的文件扩展名
    ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.svg']

    def post(self, request, pk):
        """处理封面图片上传请求"""
        try:
            try:
                comic = Comic.objects.get(pk=pk)
            except Comic.DoesNotExist:
                logger.error(f"漫画不存在: pk={pk}")
                return Response({'error': '漫画不存在'}, status=status.HTTP_404_NOT_FOUND)

            file = request.FILES.get('cover')
            if not file:
                logger.warning("未上传封面文件")
                return Response({'error': '未上传封面文件'}, status=status.HTTP_400_BAD_REQUEST)

            # 验证文件类型
            content_type = file.content_type or ''
            ext = os.path.splitext(file.name)[1].lower()
            if content_type not in self.ALLOWED_CONTENT_TYPES and ext not in self.ALLOWED_EXTENSIONS:
                return Response(
                    {'error': f'不支持的文件类型: {content_type or ext}'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            logger.info(f"收到漫画封面上传: comic={comic.title}(id={comic.id}), file={file.name}")

            # 删除旧封面文件（不删除默认占位图）
            if comic.cover_image:
                old_path = os.path.join(settings.MEDIA_ROOT, str(comic.cover_image))
                if os.path.exists(old_path):
                    try:
                        os.remove(old_path)
                        logger.debug(f"已删除旧封面: {old_path}")
                    except Exception as e:
                        logger.warning(f"删除旧封面失败: {e}")

            # 创建封面目录
            cover_dir = os.path.join(settings.MEDIA_ROOT, 'comics', 'covers')
            if not os.path.exists(cover_dir):
                os.makedirs(cover_dir)

            # 保存新封面，文件名固定为 resource_id + 扩展名
            filename = f"{comic.resource_id}{ext}"
            filepath = os.path.join(cover_dir, filename)
            relative_path = f"comics/covers/{filename}"

            with open(filepath, 'wb+') as destination:
                for chunk in file.chunks():
                    destination.write(chunk)

            comic.cover_image = relative_path
            comic.save()

            logger.info(f"漫画封面上传成功: {relative_path}")
            return Response({
                'success': True,
                'cover_url': relative_path,
                'message': '封面上传成功'
            })

        except Exception as e:
            logger.exception(f"上传漫画封面时发生未预期的错误: {str(e)}")
            return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class ComicChapterUploadImagesView(APIView):
    """漫画章节图片批量上传视图 - 用于 Django Admin 后台批量上传章节图片"""
    permission_classes = [IsAuthenticated]

    # 允许的图片 MIME 类型
    ALLOWED_CONTENT_TYPES = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif',
        'image/webp', 'image/bmp', 'image/tiff',
    ]
    # 允许的文件扩展名
    ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff']

    def post(self, request, pk):
        """处理章节图片批量上传请求（已修复并发竞态与顺序问题）"""
        try:
            try:
                chapter = ComicChapter.objects.select_related('comic').get(pk=pk)
            except ComicChapter.DoesNotExist:
                logger.error(f"章节不存在: pk={pk}")
                return Response({'error': '章节不存在'}, status=status.HTTP_404_NOT_FOUND)

            files = request.FILES.getlist('images')
            if not files:
                logger.warning("没有上传任何图片文件")
                return Response({'error': '没有上传任何图片'}, status=status.HTTP_400_BAD_REQUEST)

            comic = chapter.comic
            comic_slug = comic.slug
            if not comic_slug:
                return Response(
                    {'error': '漫画未设置 slug，无法上传，请先在漫画编辑页设置 slug'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            logger.info(f"收到章节图片上传: comic={comic.title}, chapter={chapter.chapter_number}, files={len(files)}")

            # 创建章节目录: comics/{comic_slug}/chapter_{chapter_number}/
            chapter_dir_name = f"chapter_{chapter.chapter_number}"
            chapter_dir = os.path.join(settings.MEDIA_ROOT, 'comics', comic_slug, chapter_dir_name)
            if not os.path.exists(chapter_dir):
                os.makedirs(chapter_dir)

            uploaded_count = 0
            skipped_files = []
            saved_paths = []  # 本次成功保存的路径（按入参顺序）

            for file in files:
                try:
                    # 验证文件类型
                    content_type = file.content_type or ''
                    ext = os.path.splitext(file.name)[1].lower()

                    if content_type not in self.ALLOWED_CONTENT_TYPES and ext not in self.ALLOWED_EXTENSIONS:
                        skipped_files.append(f"{file.name}({content_type})")
                        logger.warning(f"跳过不支持的文件类型: {file.name}, type={content_type}, ext={ext}")
                        continue

                    # 生成文件名，保留原文件名，重名时自动追加序号
                    base_name = os.path.splitext(file.name)[0]
                    filename = f"{base_name}{ext}"
                    counter = 1
                    while os.path.exists(os.path.join(chapter_dir, filename)):
                        filename = f"{base_name}_{counter}{ext}"
                        counter += 1

                    filepath = os.path.join(chapter_dir, filename)
                    relative_path = f"comics/{comic_slug}/{chapter_dir_name}/{filename}"

                    # 保存文件
                    with open(filepath, 'wb+') as destination:
                        for chunk in file.chunks():
                            destination.write(chunk)

                    saved_paths.append(relative_path)
                    uploaded_count += 1
                    logger.debug(f"已保存章节图片: {file.name} -> {relative_path}")

                except Exception as e:
                    logger.error(f"处理文件 {file.name} 时出错: {str(e)}")
                    skipped_files.append(f"{file.name}(错误: {str(e)})")
                    continue

            # 关键修复：使用事务 + select_for_update 锁行，避免并发覆盖
            from django.db import transaction
            with transaction.atomic():
                # 重新查询并锁定该章节行，确保其他并发请求等待
                locked_chapter = ComicChapter.objects.select_for_update().get(pk=chapter.pk)
                # 在锁内重新读取最新图片列表，再追加本次上传的图片（保持入参顺序）
                current_images = list(locked_chapter.images or [])
                current_images.extend(saved_paths)
                locked_chapter.images = current_images
                locked_chapter.save(update_fields=['images'])
                final_total = len(current_images)

            result_data = {
                'success': True,
                'uploaded_count': uploaded_count,
                'total_images': final_total
            }
            if skipped_files:
                result_data['skipped_files'] = skipped_files
                result_data['warning'] = f"成功 {uploaded_count} 张，跳过 {len(skipped_files)} 张"

            logger.info(f"章节图片上传完成: 成功 {uploaded_count}/{len(files)}, 跳过 {len(skipped_files)}, 总数 {final_total}")
            return Response(result_data)

        except Exception as e:
            logger.exception(f"上传章节图片时发生未预期的错误: {str(e)}")
            return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class ComicChapterReorderImagesView(APIView):
    """漫画章节图片排序视图 - 用于 Django Admin 后台拖拽排序后保存新顺序"""
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        """接收新的图片路径顺序并保存"""
        try:
            try:
                chapter = ComicChapter.objects.get(pk=pk)
            except ComicChapter.DoesNotExist:
                logger.error(f"章节不存在: pk={pk}")
                return Response({'error': '章节不存在'}, status=status.HTTP_404_NOT_FOUND)

            new_images = request.data.get('images')
            if not isinstance(new_images, list):
                return Response(
                    {'error': '参数 images 必须是数组'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            # 校验：新顺序中的所有路径必须存在于原列表中，避免外部注入未授权的路径
            old_images = list(chapter.images or [])
            old_set = set(old_images)
            new_set = set(new_images)
            if new_set != old_set:
                missing = old_set - new_set
                extra = new_set - old_set
                err_parts = []
                if missing:
                    err_parts.append(f"缺少 {len(missing)} 张")
                if extra:
                    err_parts.append(f"多出 {len(extra)} 张无效项")
                return Response(
                    {'error': '图片集合不一致: ' + ', '.join(err_parts)},
                    status=status.HTTP_400_BAD_REQUEST
                )

            chapter.images = new_images
            chapter.save()
            logger.info(f"章节 {chapter.id} 图片顺序已更新，共 {len(new_images)} 张")
            return Response({
                'success': True,
                'message': f'已保存 {len(new_images)} 张图片的新顺序'
            })

        except Exception as e:
            logger.exception(f"保存章节图片顺序时发生未预期的错误: {str(e)}")
            return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class AlbumReorderImagesView(APIView):
    """图集图片排序视图 - 用于 Django Admin 后台拖拽排序后保存新顺序"""
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        """接收新的图片 ID 顺序并批量更新 AlbumImage 的 order 字段"""
        try:
            try:
                album = Album.objects.get(pk=pk)
            except Album.DoesNotExist:
                logger.error(f"图集不存在: pk={pk}")
                return Response({'error': '图集不存在'}, status=status.HTTP_404_NOT_FOUND)

            new_image_ids = request.data.get('image_ids')
            if not isinstance(new_image_ids, list):
                return Response(
                    {'error': '参数 image_ids 必须是数组'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            # 校验：新顺序中的所有图片 ID 必须属于当前图集
            valid_ids = set(
                album.album_images.values_list('image_id', flat=True)
            )
            new_ids_set = set(int(i) for i in new_image_ids)
            if new_ids_set != valid_ids:
                missing = valid_ids - new_ids_set
                extra = new_ids_set - valid_ids
                err_parts = []
                if missing:
                    err_parts.append(f"缺少 {len(missing)} 张")
                if extra:
                    err_parts.append(f"多出 {len(extra)} 张无效项")
                return Response(
                    {'error': '图片集合不一致: ' + ', '.join(err_parts)},
                    status=status.HTTP_400_BAD_REQUEST
                )

            # 批量更新 order 字段
            from django.db import transaction
            with transaction.atomic():
                for order, image_id in enumerate(new_image_ids, start=1):
                    AlbumImage.objects.filter(
                        album=album, image_id=int(image_id)
                    ).update(order=order)

            logger.info(f"图集 {album.id} 图片顺序已更新，共 {len(new_image_ids)} 张")
            return Response({
                'success': True,
                'message': f'已保存 {len(new_image_ids)} 张图片的新顺序'
            })

        except Exception as e:
            logger.exception(f"保存图集图片顺序时发生未预期的错误: {str(e)}")
            return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


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
        if instance.thumbnail:
            try:
                thumb_path = instance.thumbnail.path
                if os.path.exists(thumb_path):
                    os.remove(thumb_path)
            except Exception as e:
                print(f"删除缩略图失败: {e}")
        if instance.video_file:
            try:
                video_path = instance.video_file.path
                if os.path.exists(video_path):
                    os.remove(video_path)
            except Exception as e:
                print(f"删除视频文件失败: {e}")
        return super().destroy(request, *args, **kwargs)

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

class ImageUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        file = request.FILES.get('file')
        if not file:
            return Response({'error': '没有上传文件'}, status=status.HTTP_400_BAD_REQUEST)

        allowed_types = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
        if file.content_type not in allowed_types:
            return Response({'error': '不支持的图片格式'}, status=status.HTTP_400_BAD_REQUEST)

        if file.size > 10 * 1024 * 1024:
            return Response({'error': '文件大小不能超过10MB'}, status=status.HTTP_400_BAD_REQUEST)

        ext = os.path.splitext(file.name)[1].lower()
        comic_slug = request.data.get('comic_slug')
        chapter_index = request.data.get('chapter_index', 0)
        is_cover = request.data.get('is_cover', False)
        album_resource_id = request.data.get('album_resource_id')
        is_album_cover = request.data.get('is_album_cover', False)
        resource_id = str(uuid.uuid4())

        if comic_slug:
            comic_dir = os.path.join(settings.MEDIA_ROOT, 'comics', comic_slug)
            if not os.path.exists(comic_dir):
                os.makedirs(comic_dir)

            if is_cover:
                filename = f"000{ext}"
                filepath = os.path.join(comic_dir, filename)
                relative_path = f"comics/{comic_slug}/{filename}"
            else:
                chapter_dir = os.path.join(comic_dir, f"chapter_{chapter_index}")
                if not os.path.exists(chapter_dir):
                    os.makedirs(chapter_dir)

                existing_files = [f for f in os.listdir(chapter_dir) if f.endswith(tuple(['.jpg', '.jpeg', '.png', '.gif', '.webp']))]
                max_idx = 0
                for f in existing_files:
                    try:
                        idx = int(os.path.splitext(f)[0])
                        if idx > max_idx:
                            max_idx = idx
                    except:
                        pass

                new_idx = max_idx + 1
                filename = f"{new_idx:03d}{ext}"
                filepath = os.path.join(chapter_dir, filename)
                relative_path = f"comics/{comic_slug}/chapter_{chapter_index}/{filename}"
        elif album_resource_id:
            album_dir = os.path.join(settings.MEDIA_ROOT, 'albums', album_resource_id)
            if not os.path.exists(album_dir):
                os.makedirs(album_dir)

            if is_album_cover:
                filename = f"cover{ext}"
                filepath = os.path.join(album_dir, filename)
                relative_path = f"albums/{album_resource_id}/{filename}"
            else:
                filename = f"{resource_id}{ext}"
                filepath = os.path.join(album_dir, filename)
                relative_path = f"albums/{album_resource_id}/{filename}"
        else:
            filename = f"{resource_id}{ext}"

            images_dir = os.path.join(settings.MEDIA_ROOT, 'images')
            if not os.path.exists(images_dir):
                os.makedirs(images_dir)

            filepath = os.path.join(images_dir, filename)
            relative_path = f"images/{filename}"

        with open(filepath, 'wb+') as destination:
            for chunk in file.chunks():
                destination.write(chunk)

        return Response({'path': relative_path, 'resource_id': resource_id}, status=status.HTTP_201_CREATED)

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
        video_type = request.data.get('type', 'video')

        if video_type == 'special':
            base_dir = 'special'
        else:
            base_dir = 'videos'

        videos_dir = os.path.join(settings.MEDIA_ROOT, base_dir, 'files')
        if not os.path.exists(videos_dir):
            os.makedirs(videos_dir)

        resource_id = str(uuid.uuid4())
        filename = f"{resource_id}{ext}"
        filepath = os.path.join(videos_dir, filename)
        relative_path = f"{base_dir}/files/{filename}"

        try:
            with open(filepath, 'wb+') as destination:
                for chunk in file.chunks():
                    destination.write(chunk)
        except Exception as e:
            return Response({'error': '保存文件失败', 'detail': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        thumbnail_filename = f"{resource_id}.jpg"
        thumbnail_path = os.path.join(settings.MEDIA_ROOT, base_dir, 'thumbnails', thumbnail_filename)
        thumbnail_relative_path = f"{base_dir}/thumbnails/{thumbnail_filename}"

        if not os.path.exists(os.path.join(settings.MEDIA_ROOT, base_dir, 'thumbnails')):
            os.makedirs(os.path.join(settings.MEDIA_ROOT, base_dir, 'thumbnails'))

        try:
            import subprocess
            result = subprocess.run(
                ['ffmpeg', '-i', filepath, '-ss', '00:00:01', '-vframes', '1', '-q:v', '2', thumbnail_path, '-y'],
                capture_output=True,
                text=True,
                timeout=30
            )
            if result.returncode == 0 and os.path.exists(thumbnail_path):
                return Response({'path': relative_path, 'thumbnail': thumbnail_relative_path, 'resource_id': resource_id}, status=status.HTTP_201_CREATED)
            else:
                return Response({'path': relative_path, 'resource_id': resource_id}, status=status.HTTP_201_CREATED)
        except Exception as e:
            print(f"生成视频缩略图失败: {e}")
            return Response({'path': relative_path, 'resource_id': resource_id}, status=status.HTTP_201_CREATED)

class VideoFolderUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        files = request.FILES.getlist('files')
        if not files or len(files) == 0:
            return Response({'error': '没有上传文件', 'detail': 'FILES中不存在files字段'}, status=status.HTTP_400_BAD_REQUEST)

        video_type = request.data.get('type', 'video')
        if video_type == 'special':
            base_dir = 'special'
        else:
            base_dir = 'videos'

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

        video_uuid = uuid.uuid4().hex
        video_dir = os.path.join(settings.MEDIA_ROOT, base_dir, 'files', video_uuid)
        if not os.path.exists(video_dir):
            os.makedirs(video_dir)

        m3u8_path = None
        for file in files:
            filename = os.path.basename(file.name)
            filepath = os.path.join(video_dir, filename)
            relative_path = f"{base_dir}/files/{video_uuid}/{filename}"
            
            if filename.lower().endswith('.m3u8'):
                m3u8_path = relative_path
            
            try:
                with open(filepath, 'wb+') as destination:
                    for chunk in file.chunks():
                        destination.write(chunk)
            except Exception as e:
                import shutil
                if os.path.exists(video_dir):
                    shutil.rmtree(video_dir)
                return Response({'error': '保存文件失败', 'detail': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        thumbnail_path = None
        if ts_files:
            first_ts = ts_files[0]
            ts_filename = os.path.basename(first_ts.name)
            ts_filepath = os.path.join(video_dir, ts_filename)
            
            thumbnail_filename = f"{video_uuid}.jpg"
            thumbnail_filepath = os.path.join(settings.MEDIA_ROOT, base_dir, 'thumbnails', thumbnail_filename)
            
            if not os.path.exists(os.path.join(settings.MEDIA_ROOT, base_dir, 'thumbnails')):
                os.makedirs(os.path.join(settings.MEDIA_ROOT, base_dir, 'thumbnails'))
            
            try:
                import subprocess
                result = subprocess.run(
                    ['ffmpeg', '-i', ts_filepath, '-ss', '00:00:00', '-vframes', '1', '-q:v', '2', thumbnail_filepath, '-y'],
                    capture_output=True,
                    text=True,
                    timeout=30
                )
                if result.returncode == 0 and os.path.exists(thumbnail_filepath):
                    thumbnail_path = f"{base_dir}/thumbnails/{thumbnail_filename}"
            except Exception as e:
                print(f"生成视频缩略图失败: {e}")

        return Response({
            'm3u8_path': m3u8_path,
            'thumbnail': thumbnail_path,
            'resource_id': video_uuid,
            'message': 'm3u8视频文件夹上传成功'
        }, status=status.HTTP_201_CREATED)

class VideoThumbnailUploadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        file = request.FILES.get('file')
        if not file:
            return Response({'error': '没有上传文件'}, status=status.HTTP_400_BAD_REQUEST)

        allowed_types = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
        if file.content_type not in allowed_types:
            return Response({'error': '不支持的图片格式'}, status=status.HTTP_400_BAD_REQUEST)

        if file.size > 5 * 1024 * 1024:
            return Response({'error': '文件大小不能超过5MB'}, status=status.HTTP_400_BAD_REQUEST)

        ext = os.path.splitext(file.name)[1].lower()

        videos_dir = os.path.join(settings.MEDIA_ROOT, 'videos', 'thumbnails')
        if not os.path.exists(videos_dir):
            os.makedirs(videos_dir)

        resource_id = str(uuid.uuid4())
        filename = f"{resource_id}{ext}"
        filepath = os.path.join(videos_dir, filename)
        relative_path = f"videos/thumbnails/{filename}"

        with open(filepath, 'wb+') as destination:
            for chunk in file.chunks():
                destination.write(chunk)

        return Response({'path': relative_path, 'resource_id': resource_id}, status=status.HTTP_201_CREATED)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def sync_media_view(request):
    try:
        synced_comics = 0
        synced_videos = 0
        synced_images = 0
        synced_albums = 0
        comics_dir = os.path.join(settings.MEDIA_ROOT, 'comics')
        videos_dir = os.path.join(settings.MEDIA_ROOT, 'videos')
        images_dir = os.path.join(settings.MEDIA_ROOT, 'images')

        if os.path.exists(comics_dir):
            for comic_slug in os.listdir(comics_dir):
                comic_path = os.path.join(comics_dir, comic_slug)
                if not os.path.isdir(comic_path):
                    continue
                
                comic, created = Comic.objects.get_or_create(
                    slug=comic_slug,
                    defaults={
                        'title': comic_slug.replace('-', ' ').replace('_', ' ').title(),
                        'description': '同步创建的漫画',
                        'author': '未知'
                    }
                )
                
                if created:
                    synced_comics += 1
                    manga_cat = Category.objects.filter(slug='comics').first()
                    if manga_cat:
                        comic.categories.add(manga_cat)
                
                if not comic.resource_id:
                    comic.resource_id = str(uuid.uuid4())
                    comic.save()
                
                loose_images = []
                chapter_dirs = []
                
                for item in os.listdir(comic_path):
                    item_path = os.path.join(comic_path, item)
                    if os.path.isdir(item_path):
                        chapter_match = re.match(r'chapter_(\d+)', item)
                        if chapter_match:
                            chapter_dirs.append((item, int(chapter_match.group(1)), item_path))
                    elif item.lower().endswith(('.jpg', '.jpeg', '.png', '.gif', '.webp')):
                        loose_images.append(f'comics/{comic_slug}/{item}')
                
                for chapter_dir, chapter_index, chapter_path in sorted(chapter_dirs, key=lambda x: x[1]):
                    chapter, chapter_created = ComicChapter.objects.get_or_create(
                        comic=comic,
                        chapter_number=chapter_index,
                        defaults={'title': f'第{chapter_index}章'}
                    )
                    
                    if chapter_created:
                        images = []
                        for img_file in sorted(os.listdir(chapter_path)):
                            if img_file.lower().endswith(('.jpg', '.jpeg', '.png', '.gif', '.webp')):
                                img_relative = f'comics/{comic_slug}/{chapter_dir}/{img_file}'
                                images.append(img_relative)
                        
                        if images:
                            chapter.images = images
                            chapter.save()
                        
                        if not comic.cover_image and images:
                            comic.cover_image = images[0]
                            comic.save()
                
                for ext in ['.jpg', '.jpeg', '.png', '.gif', '.webp']:
                    cover_path = os.path.join(comic_path, f'000{ext}')
                    if os.path.exists(cover_path):
                        comic.cover_image = f'comics/{comic_slug}/000{ext}'
                        comic.save()
                        break
                
                if not comic.cover_image and loose_images:
                    comic.cover_image = loose_images[0]
                    comic.save()

        for base_dir, video_type in [('videos', 'video'), ('special', 'special')]:
            media_dir = os.path.join(settings.MEDIA_ROOT, base_dir)
            if os.path.exists(media_dir):
                media_files_dir = os.path.join(media_dir, 'files')
                media_thumbnails_dir = os.path.join(media_dir, 'thumbnails')
                
                if os.path.exists(media_files_dir):
                    for video_file in os.listdir(media_files_dir):
                        video_path = os.path.join(media_files_dir, video_file)
                        if os.path.isfile(video_path):
                            if video_file.lower().endswith(('.mp4', '.avi', '.mov', '.mkv', '.webm')):
                                resource_id = os.path.splitext(video_file)[0]
                                
                                video, created = Video.objects.get_or_create(
                                    resource_id=resource_id,
                                    defaults={
                                        'title': video_file,
                                        'description': '同步创建的视频',
                                        'duration': 0,
                                        'video_type': video_type
                                    }
                                )
                                
                                if created:
                                    synced_videos += 1
                                
                                if video.video_type != video_type:
                                    video.video_type = video_type
                                    video.save()
                                
                                if not video.video_file:
                                    video.video_file = f'{base_dir}/files/{video_file}'
                                    video.save()
                                
                                thumbnail_name = f"{resource_id}.jpg"
                                thumbnail_path = os.path.join(media_thumbnails_dir, thumbnail_name)
                                
                                if os.path.exists(thumbnail_path):
                                    video.thumbnail = f'{base_dir}/thumbnails/{thumbnail_name}'
                                    video.save()
                
                for video_file in os.listdir(media_dir):
                    video_path = os.path.join(media_dir, video_file)
                    if os.path.isfile(video_path) and not os.path.dirname(video_path).endswith('files') and not os.path.dirname(video_path).endswith('thumbnails'):
                        if video_file.lower().endswith(('.mp4', '.avi', '.mov', '.mkv', '.webm')):
                            video_slug = os.path.splitext(video_file)[0]
                            
                            video, created = Video.objects.get_or_create(
                                slug=video_slug,
                                defaults={
                                    'title': video_slug,
                                    'description': '同步创建的视频',
                                    'duration': 0,
                                    'video_type': video_type
                                }
                            )
                            
                            if created:
                                synced_videos += 1
                            
                            if video.video_type != video_type:
                                video.video_type = video_type
                                video.save()
                            
                            if not video.resource_id:
                                video.resource_id = str(uuid.uuid4())
                                video.save()
                            
                            if not video.video_file:
                                video.video_file = f'{base_dir}/{video_file}'
                                video.save()
                            
                            thumbnail_name = os.path.splitext(video_file)[0] + '.jpg'
                            thumbnail_path = os.path.join(media_dir, thumbnail_name)
                            
                            if os.path.exists(thumbnail_path):
                                video.thumbnail = f'{base_dir}/{thumbnail_name}'
                                video.save()

        if os.path.exists(images_dir):
            for image_file in os.listdir(images_dir):
                image_path = os.path.join(images_dir, image_file)
                if os.path.isfile(image_path):
                    if image_file.lower().endswith(('.jpg', '.jpeg', '.png', '.gif', '.webp')):
                        resource_id = os.path.splitext(image_file)[0]
                        
                        image, created = Image.objects.get_or_create(
                            resource_id=resource_id,
                            defaults={
                                'title': resource_id,
                                'description': '同步创建的图片',
                                'resource_type': 'image'
                            }
                        )
                        
                        if created:
                            synced_images += 1
                        
                        if not image.image_file:
                            image.image_file = f'images/{image_file}'
                            image.save()

        return Response({
            'message': f'媒体文件同步成功',
            'synced_comics': synced_comics,
            'synced_videos': synced_videos,
            'synced_images': synced_images,
            'synced_albums': synced_albums
        }, status=status.HTTP_200_OK)
    except Exception as e:
        import traceback
        error_details = traceback.format_exc()
        print(f"同步媒体文件错误: {error_details}")
        return Response({'error': str(e), 'details': error_details[:500]}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@permission_classes([IsAdminPermission])
def cleanup_temp_files(request):
    try:
        comic_slug = request.data.get('comic_slug')
        if comic_slug:
            comic_dir = os.path.join(settings.MEDIA_ROOT, 'comics', comic_slug)
            if os.path.exists(comic_dir):
                import shutil
                shutil.rmtree(comic_dir)
                return Response({'message': '临时文件清理成功'}, status=status.HTTP_200_OK)
        return Response({'message': '没有需要清理的临时文件'}, status=status.HTTP_200_OK)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

class CategoryListView(generics.ListAPIView):
    """标签列表视图 - 仅读取，创建使用CategoryCreateView(管理员权限)"""
    serializer_class = CategorySerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        """管理员返回全部标签，普通用户返回普通权限标签（隐私模式视为普通用户）"""
        if is_effective_admin(self.request):
            return Category.objects.all()
        return Category.objects.filter(permission_level='regular')

class CategoryDetailView(generics.RetrieveAPIView):
    """标签详情视图 - 仅读取，更新/删除使用管理员视图"""
    serializer_class = CategorySerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        """管理员返回全部标签，普通用户返回普通权限标签（隐私模式视为普通用户）"""
        if is_effective_admin(self.request):
            return Category.objects.all()
        return Category.objects.filter(permission_level='regular')

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

class UserListView(generics.ListAPIView):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [IsAdminPermission]

class UserUpdateView(generics.UpdateAPIView):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [IsAdminPermission]
    http_method_names = ['put', 'patch', 'options']

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

class UserProfileView(APIView):
    permission_classes = [IsAuthenticated]

    # 允许上传的头像扩展名与大小上限（2MB）
    ALLOWED_AVATAR_EXTS = {'.jpg', '.jpeg', '.png', '.webp', '.gif'}
    MAX_AVATAR_SIZE = 2 * 1024 * 1024

    def _serialize(self, request, user):
        """序列化当前用户资料（头像拼成绝对地址，附带版本号防缓存）"""
        profile = getattr(user, 'profile', None)
        user_type = profile.user_type if profile else 'user'
        # 注册时间/最近登录直接取 Django User 自带字段
        created_at = user.date_joined
        last_login = user.last_login
        nickname = profile.nickname if profile else ''

        avatar_url = ''
        if profile and profile.avatar:
            # 返回相对地址（/media/...），由客户端按自身配置的服务器域名拼接，
            # 保证自定义服务器地址或局域网直连时也能访问并附带鉴权头
            avatar_url = profile.avatar.url
            # 头像更新后地址不变，用文件修改时间做版本号，强制客户端刷新缓存
            try:
                import os
                mtime = int(os.path.getmtime(profile.avatar.path))
                avatar_url = f'{avatar_url}?v={mtime}'
            except Exception:
                pass

        return {
            'id': user.id,
            'username': user.username,
            'email': user.email,
            'user_type': user_type,
            'nickname': nickname,
            'avatar': avatar_url,
            'created_at': created_at,
            'last_login': last_login
        }

    def get(self, request):
        return Response(self._serialize(request, request.user))

    def patch(self, request):
        """修改个人资料：支持昵称（nickname 字段）与头像（avatar 文件）"""
        from api.models import Profile
        user = request.user
        # 资料行可能尚未创建（老用户），这里懒创建
        profile, _ = Profile.objects.get_or_create(user=user)

        # 昵称修改：去除首尾空白并限制长度
        if 'nickname' in request.data:
            nickname = (request.data.get('nickname') or '').strip()
            if len(nickname) > 30:
                return Response({'error': '昵称最长 30 个字符'}, status=status.HTTP_400_BAD_REQUEST)
            profile.nickname = nickname

        # 头像上传：校验扩展名与大小
        avatar_file = request.FILES.get('avatar')
        if avatar_file is not None:
            import os
            ext = os.path.splitext(avatar_file.name)[1].lower()
            if ext not in self.ALLOWED_AVATAR_EXTS:
                return Response({'error': '头像仅支持 jpg/png/webp/gif 格式'},
                                status=status.HTTP_400_BAD_REQUEST)
            if avatar_file.size > self.MAX_AVATAR_SIZE:
                return Response({'error': '头像大小不能超过 2MB'},
                                status=status.HTTP_400_BAD_REQUEST)
            profile.avatar = avatar_file

        profile.save()
        return Response(self._serialize(request, user))

class UserStatsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        comics_count = Comic.objects.filter(uploader=user).count()
        videos_count = Video.objects.filter(uploader=user).count()
        notes_count = Note.objects.filter(user=user).count()
        cloud_files_count = CloudFile.objects.filter(user=user).count()
        
        return Response({
            'comics_count': comics_count,
            'videos_count': videos_count,
            'notes_count': notes_count,
            'cloud_files_count': cloud_files_count
        })

import hashlib
import hmac

OBFUSCATION_MASK = bytes.fromhex('3a7f2e9c4b8d1a6e3b0c5d2a7f8e1b4c')
MASTER_KEY_1 = bytes.fromhex('5a8f2d7c3e9a1b6f4a0d5c3b7e8f2c1d')
ARGON2_SALT = bytes.fromhex('a1b2c3d4e5f60708090a0b0c0d0e0f10')
ARGON2_ITERATIONS = 100000
AES_ENCRYPTION_KEY = bytes.fromhex('7c9a3b8f2d6e1a4c5f0b7e3d8c1f6a4b2e9d5a0c7f3b8e2d6c1b4a5f0d7e3c')
HMAC_VERIFICATION_KEY = bytes.fromhex('2d6f1c4b5a0f7e3c8d2a5f0e7b4a3d8c1f6e5b0c7a4d2e9f6c1b5a0f7d3e8b')
FIXED_IV = bytes.fromhex('00112233445566778899aabbccddeeff')
ELEVATION_L5_HMAC = '263c8b566c6004e2b57a6cedb9fc11439da5de7709d08fa889c1c94ffe76f316'

def l1_byte_obfuscation(data: bytes, mask: bytes) -> bytes:
    result = bytearray(len(data))
    for i in range(len(data)):
        result[i] = data[i] ^ mask[i % len(mask)]
    return bytes(result)

def l2_sha3_512(data: bytes, key: bytes) -> bytes:
    combined = data + key
    return hashlib.sha3_512(combined).digest()

def l3_kdf(data: bytes, salt: bytes, iterations: int) -> bytes:
    return hashlib.pbkdf2_hmac('sha256', data, salt, iterations, dklen=32)

def l4_aes_encrypt(data: bytes, key: bytes, iv: bytes) -> bytes:
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    from cryptography.hazmat.backends import default_backend

    key_32 = key[:32] if len(key) >= 32 else key.ljust(32, b'\x00')

    cipher = Cipher(
        algorithms.AES(key_32),
        modes.CBC(iv),
        backend=default_backend()
    )
    encryptor = cipher.encryptor()

    padding_length = 16 - (len(data) % 16)
    padded_data = data + bytes([padding_length] * padding_length)

    ciphertext = encryptor.update(padded_data) + encryptor.finalize()
    return iv + ciphertext

def l5_hmac_compute(data: bytes, key: bytes) -> bytes:
    return hmac.new(key, data, hashlib.sha256).digest()

def verify_5layer_elevation_code(input_code: str) -> bool:
    try:
        password_bytes = input_code.encode('utf-8')

        l1_result = l1_byte_obfuscation(password_bytes, OBFUSCATION_MASK)
        l2_result = l2_sha3_512(l1_result, MASTER_KEY_1)
        l3_result = l3_kdf(l2_result, ARGON2_SALT, ARGON2_ITERATIONS)
        l4_result = l4_aes_encrypt(l3_result, AES_ENCRYPTION_KEY, FIXED_IV)
        computed_l5 = l5_hmac_compute(l3_result, HMAC_VERIFICATION_KEY)

        stored_hmac = bytes.fromhex(ELEVATION_L5_HMAC)
        return hmac.compare_digest(computed_l5, stored_hmac)

    except Exception as e:
        print(f"5层授权码验证错误: {e}")
        return False

class UserElevationView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user

        if hasattr(user, 'profile') and user.profile.user_type == 'admin':
            return Response({
                'error': '权限已是管理员',
                'detail': '当前账户已经是管理员权限，无需提升'
            }, status=status.HTTP_400_BAD_REQUEST)

        admin_code = request.data.get('admin_code')

        if not admin_code:
            return Response({
                'error': '缺少授权码',
                'detail': '请输入管理员授权码'
            }, status=status.HTTP_400_BAD_REQUEST)

        if not verify_5layer_elevation_code(admin_code):
            return Response({
                'error': '授权码无效',
                'detail': '请输入有效的管理员授权码'
            }, status=status.HTTP_400_BAD_REQUEST)

        try:
            profile, created = Profile.objects.get_or_create(user=user)
            profile.user_type = 'admin'
            profile.save()

            return Response({
                'success': True,
                'message': '权限提升成功',
                'detail': '您的账户已成功升级为管理员权限'
            }, status=status.HTTP_200_OK)
        except Exception as e:
            return Response({
                'error': '权限提升失败',
                'detail': str(e)
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

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
        
        def delete_recursive(file_obj):
            children = list(file_obj.children.all())
            for child in children:
                delete_recursive(child)
            if file_obj.file_path and file_obj.file_type == 'file':
                try:
                    file_path = file_obj.file_path.path
                    if os.path.exists(file_path):
                        os.remove(file_path)
                except Exception as e:
                    print(f"删除文件失败: {e}")
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