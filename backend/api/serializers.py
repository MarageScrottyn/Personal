from rest_framework import serializers
from django.conf import settings
from django.contrib.auth.models import User
from .models import Category, Comic, ComicChapter, Video, Profile, Note, CloudFile, Image, Album


def build_media_url(request, path):
    """将媒体相对路径转为完整URL"""
    if not path:
        return ''
    path = str(path)
    if path.startswith('http://') or path.startswith('https://'):
        return path
    if path.startswith('/media/'):
        normalized = path
    elif path.startswith('media/'):
        normalized = '/' + path
    else:
        normalized = '/media/' + path
    if request:
        return request.build_absolute_uri(normalized)
    return normalized

class UserSerializer(serializers.ModelSerializer):
    profile = serializers.SerializerMethodField()
    user_type = serializers.CharField(write_only=True, required=False)

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'password', 'profile', 'user_type']
        extra_kwargs = {
            'password': {'write_only': True, 'required': False},
            'username': {'required': False},
            'email': {'required': False}
        }

    def get_profile(self, obj):
        if hasattr(obj, 'profile'):
            return {
                'user_type': obj.profile.user_type
            }
        return {'user_type': 'user'}

    def create(self, validated_data):
        user_type = validated_data.pop('user_type', 'user')
        user = User.objects.create_user(**validated_data)
        Profile.objects.create(user=user, user_type=user_type)
        return user

    def update(self, instance, validated_data):
        if 'password' in validated_data:
            password = validated_data.pop('password')
            if password:
                instance.set_password(password)

        user_type = validated_data.pop('user_type', None)
        if user_type:
            if not hasattr(instance, 'profile'):
                Profile.objects.create(user=instance, user_type=user_type)
            else:
                instance.profile.user_type = user_type
                instance.profile.save()

        return super().update(instance, validated_data)

class ImageSerializer(serializers.ModelSerializer):
    image_file = serializers.SerializerMethodField()
    category_names = serializers.SerializerMethodField()
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True, required=False)

    class Meta:
        model = Image
        fields = ['id', 'resource_id', 'image_file', 'title', 'description', 'category_names', 'categories', 'resource_type', 'album_id', 'created_at', 'updated_at']
        read_only_fields = ['id', 'resource_id', 'created_at', 'updated_at']

    def get_image_file(self, obj):
        if not obj.image_file:
            return ''
        img_path = str(obj.image_file)
        if img_path.startswith('http://') or img_path.startswith('https://'):
            return img_path
        # 规范化路径
        if img_path.startswith('/media/'):
            normalized_path = img_path
        elif img_path.startswith('media/'):
            normalized_path = '/' + img_path
        else:
            normalized_path = '/media/' + img_path
        # 构建完整 URL
        request = self.context.get('request')
        if request:
            return request.build_absolute_uri(normalized_path)
        return normalized_path

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

    def create(self, validated_data):
        categories = validated_data.pop('categories', [])
        image = super().create(validated_data)
        if categories:
            image.categories.set(categories)
        return image

    def update(self, instance, validated_data):
        categories = validated_data.pop('categories', [])
        instance = super().update(instance, validated_data)
        if categories:
            instance.categories.set(categories)
        return instance

class AlbumSerializer(serializers.ModelSerializer):
    """图集序列化器 - 按上传顺序返回图片"""
    cover_image = serializers.SerializerMethodField()
    cover_image_id = serializers.IntegerField(write_only=True, required=False, source='cover_image')
    images = serializers.SerializerMethodField()
    image_urls = serializers.SerializerMethodField()
    category_names = serializers.SerializerMethodField()
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True, required=False)

    class Meta:
        model = Album
        fields = ['id', 'resource_id', 'title', 'description', 'cover_image', 'cover_image_id', 'images', 'image_urls', 'category_names', 'categories', 'author', 'created_at', 'updated_at']
        read_only_fields = ['id', 'resource_id', 'created_at', 'updated_at']

    def _get_request(self):
        """获取当前请求对象"""
        return self.context.get('request')

    def _build_media_url(self, path):
        """将相对路径构建为完整URL"""
        if path.startswith('http://') or path.startswith('https://'):
            return path
        # 规范化路径
        if path.startswith('/media/'):
            normalized_path = path
        elif path.startswith('media/'):
            normalized_path = '/' + path
        else:
            normalized_path = '/media/' + path
        # 使用 request 构建完整 URL
        request = self._get_request()
        if request:
            return request.build_absolute_uri(normalized_path)
        return normalized_path

    def get_cover_image(self, obj):
        """获取封面图片URL（完整路径）"""
        if obj.cover_image:
            img_path = str(obj.cover_image.image_file)
            return self._build_media_url(img_path)
        return ''

    def _format_image_path(self, image):
        """格式化图片路径为完整URL"""
        img_path = str(image.image_file)
        return self._build_media_url(img_path)

    def get_images(self, obj):
        """获取按上传顺序排列的图片列表，兼容旧数据"""
        # 先检查是否有 AlbumImage 排序记录
        image_entries = obj.album_images.select_related('image').order_by('order')
        if image_entries.exists():
            # 有排序记录，按 order 返回
            result = []
            for entry in image_entries:
                img = entry.image
                result.append({
                    'id': img.id,
                    'resource_id': img.resource_id,
                    'title': img.title,
                    'path': self._format_image_path(img),
                    'order': entry.order
                })
            return result
        else:
            # 旧数据没有排序记录，直接返回 M2M 数据
            result = []
            for idx, img in enumerate(obj.images.all()):
                result.append({
                    'id': img.id,
                    'resource_id': img.resource_id,
                    'title': img.title,
                    'path': self._format_image_path(img),
                    'order': idx
                })
            return result

    def get_image_urls(self, obj):
        """获取按上传顺序排列的图片URL列表"""
        urls = []
        image_entries = obj.album_images.select_related('image').order_by('order')
        if image_entries.exists():
            for entry in image_entries:
                urls.append(self._format_image_path(entry.image))
        else:
            for img in obj.images.all():
                urls.append(self._format_image_path(img))
        return urls

    def get_category_names(self, obj):
        """获取标签名称列表"""
        return [category.name for category in obj.categories.all()]

    def create(self, validated_data):
        """创建图集"""
        cover_image_data = validated_data.pop('cover_image', None)
        categories = validated_data.pop('categories', [])
        
        if cover_image_data and isinstance(cover_image_data, int):
            try:
                validated_data['cover_image'] = Image.objects.get(id=cover_image_data)
            except Image.DoesNotExist:
                validated_data['cover_image'] = None
        
        album = super().create(validated_data)
        
        if categories:
            album.categories.set(categories)
        
        return album

    def update(self, instance, validated_data):
        """更新图集"""
        cover_image_data = validated_data.pop('cover_image', None)
        categories = validated_data.pop('categories', [])
        
        if cover_image_data and isinstance(cover_image_data, int):
            try:
                validated_data['cover_image'] = Image.objects.get(id=cover_image_data)
            except Image.DoesNotExist:
                validated_data['cover_image'] = None
        
        instance = super().update(instance, validated_data)
        
        if categories:
            instance.categories.set(categories)
        
        return instance

class ComicChapterSerializer(serializers.ModelSerializer):
    id = serializers.IntegerField(read_only=True)
    created_at = serializers.DateTimeField(read_only=True)
    images = serializers.ListField(child=serializers.CharField(), required=False, default=list)

    class Meta:
        model = ComicChapter
        fields = ['id', 'title', 'chapter_number', 'images', 'created_at']

    def to_representation(self, instance):
        data = super().to_representation(instance)
        if instance.images:
            formatted_images = []
            for img_path in instance.images:
                img_path = str(img_path)
                if img_path.startswith('http://') or img_path.startswith('https://'):
                    formatted_images.append(img_path)
                elif img_path.startswith('/media/'):
                    formatted_images.append(img_path)
                elif img_path.startswith('media/'):
                    formatted_images.append('/' + img_path)
                else:
                    formatted_images.append('/media/' + img_path)
            data['images'] = formatted_images
        return data

class ComicChapterReadSerializer(serializers.ModelSerializer):
    id = serializers.IntegerField(read_only=True)
    created_at = serializers.DateTimeField(read_only=True)
    images = serializers.SerializerMethodField()

    class Meta:
        model = ComicChapter
        fields = ['id', 'title', 'chapter_number', 'images', 'created_at']

    def get_images(self, obj):
        if not obj.images:
            return []
        request = self.context.get('request')
        return [build_media_url(request, p) for p in obj.images]

class ComicListSerializer(serializers.ModelSerializer):
    category_names = serializers.SerializerMethodField()
    categories = serializers.SerializerMethodField()
    cover_image = serializers.SerializerMethodField()

    class Meta:
        model = Comic
        fields = ['id', 'resource_id', 'title', 'slug', 'description', 'cover_image', 'category_names', 'categories', 'author', 'created_at']

    def get_cover_image(self, obj):
        request = self.context.get('request')
        if obj.cover_image:
            return build_media_url(request, obj.cover_image)
        chapters = obj.chapters.all()
        if chapters:
            first_chapter = chapters.first()
            if first_chapter.images:
                for img_path in first_chapter.images:
                    if img_path:
                        return build_media_url(request, img_path)
        return ''

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

    def get_categories(self, obj):
        return [category.id for category in obj.categories.all()]

class ComicDetailSerializer(serializers.ModelSerializer):
    chapters = ComicChapterSerializer(many=True)
    category_names = serializers.SerializerMethodField()
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    cover_image = serializers.SerializerMethodField()

    class Meta:
        model = Comic
        fields = ['id', 'resource_id', 'title', 'slug', 'description', 'cover_image', 'category_names', 'categories', 'author', 'chapters', 'created_at', 'updated_at']

    def get_cover_image(self, obj):
        request = self.context.get('request')
        if obj.cover_image:
            return build_media_url(request, obj.cover_image)
        chapters = obj.chapters.all()
        if chapters:
            first_chapter = chapters.first()
            if first_chapter.images:
                for img_path in first_chapter.images:
                    if img_path:
                        return build_media_url(request, img_path)
        return ''

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

class ComicCreateUpdateSerializer(serializers.ModelSerializer):
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    cover_image = serializers.CharField(required=False, allow_blank=True, max_length=200)
    chapters = ComicChapterSerializer(many=True, required=False)

    class Meta:
        model = Comic
        fields = ['resource_id', 'title', 'slug', 'description', 'cover_image', 'categories', 'author', 'chapters']

    def create(self, validated_data):
        chapters_data = validated_data.pop('chapters', [])
        categories = validated_data.pop('categories', [])

        cover_image_path = validated_data.pop('cover_image', None)
        if cover_image_path:
            if cover_image_path.startswith('/media/'):
                cover_image_path = cover_image_path[7:]
            elif cover_image_path.startswith('media/'):
                cover_image_path = cover_image_path[6:]
            validated_data['cover_image'] = cover_image_path

        comic = super().create(validated_data)
        
        if categories:
            comic.categories.set(categories)
        
        for chapter_data in chapters_data:
            ComicChapter.objects.create(comic=comic, **chapter_data)
        
        return comic

    def update(self, instance, validated_data):
        chapters_data = validated_data.pop('chapters', [])
        categories = validated_data.pop('categories', [])

        cover_image_path = validated_data.pop('cover_image', None)
        if cover_image_path:
            if cover_image_path.startswith('/media/'):
                cover_image_path = cover_image_path[7:]
            elif cover_image_path.startswith('media/'):
                cover_image_path = cover_image_path[6:]
            validated_data['cover_image'] = cover_image_path

        instance = super().update(instance, validated_data)
        
        if categories:
            instance.categories.set(categories)
        
        if chapters_data:
            instance.chapters.all().delete()
            for chapter_data in chapters_data:
                ComicChapter.objects.create(comic=instance, **chapter_data)
        
        return instance

class VideoListSerializer(serializers.ModelSerializer):
    category_names = serializers.SerializerMethodField()
    categories = serializers.SerializerMethodField()
    thumbnail = serializers.SerializerMethodField()
    video_file = serializers.SerializerMethodField()

    class Meta:
        model = Video
        fields = ['id', 'resource_id', 'title', 'slug', 'description', 'thumbnail', 'video_file', 'm3u8_path', 'category_names', 'categories', 'duration', 'video_type', 'tags', 'created_at']

    def get_thumbnail(self, obj):
        if not obj.thumbnail:
            return ''
        request = self.context.get('request')
        return build_media_url(request, obj.thumbnail)

    def get_video_file(self, obj):
        if not obj.video_file:
            return ''
        request = self.context.get('request')
        return build_media_url(request, obj.video_file)

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

    def get_categories(self, obj):
        return [category.id for category in obj.categories.all()]

class VideoCreateUpdateSerializer(serializers.ModelSerializer):
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    thumbnail = serializers.CharField(required=False, allow_blank=True, max_length=200)
    video_file = serializers.CharField(required=False, allow_blank=True, max_length=200)

    class Meta:
        model = Video
        fields = ['resource_id', 'title', 'slug', 'description', 'thumbnail', 'video_file', 'm3u8_path', 'categories', 'duration', 'video_type', 'tags']

    def _convert_path_to_file(self, file_path):
        import os
        from django.core.files.base import ContentFile

        if not file_path:
            return None

        if file_path.startswith('/media/'):
            file_path = file_path[7:]
        elif file_path.startswith('media/'):
            file_path = file_path[6:]

        full_path = os.path.join(settings.MEDIA_ROOT, file_path)
        if os.path.exists(full_path):
            with open(full_path, 'rb') as f:
                file_content = ContentFile(f.read())
                file_content.name = os.path.basename(file_path)
                return file_content
        return None

    def create(self, validated_data):
        categories = validated_data.pop('categories', [])

        thumbnail_path = validated_data.pop('thumbnail', None)
        video_file_path = validated_data.pop('video_file', None)

        if thumbnail_path:
            validated_data['thumbnail'] = self._convert_path_to_file(thumbnail_path)
        if video_file_path:
            validated_data['video_file'] = self._convert_path_to_file(video_file_path)

        video = super().create(validated_data)
        if categories:
            video.categories.set(categories)
        return video

    def update(self, instance, validated_data):
        categories = validated_data.pop('categories', [])

        thumbnail_path = validated_data.pop('thumbnail', None)
        video_file_path = validated_data.pop('video_file', None)

        if thumbnail_path:
            validated_data['thumbnail'] = self._convert_path_to_file(thumbnail_path)
        if video_file_path:
            validated_data['video_file'] = self._convert_path_to_file(video_file_path)

        instance = super().update(instance, validated_data)
        if categories:
            instance.categories.set(categories)
        return instance

class VideoDetailSerializer(serializers.ModelSerializer):
    category_names = serializers.SerializerMethodField()
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    thumbnail = serializers.SerializerMethodField()
    video_file = serializers.SerializerMethodField()

    class Meta:
        model = Video
        fields = ['id', 'resource_id', 'title', 'slug', 'description', 'thumbnail', 'video_file', 'm3u8_path', 'category_names', 'categories', 'duration', 'video_type', 'tags', 'created_at', 'updated_at']

    def get_thumbnail(self, obj):
        if not obj.thumbnail:
            return ''
        request = self.context.get('request')
        return build_media_url(request, obj.thumbnail)

    def get_video_file(self, obj):
        if not obj.video_file:
            return ''
        request = self.context.get('request')
        return build_media_url(request, obj.video_file)

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

class CategorySerializer(serializers.ModelSerializer):
    children = serializers.SerializerMethodField()
    parent_name = serializers.CharField(source='parent.name', read_only=True, allow_null=True)

    class Meta:
        model = Category
        fields = ['id', 'name', 'slug', 'parent', 'parent_name', 'permission_level', 'children']

    def get_children(self, obj):
        if obj.children.exists():
            return CategorySerializer(obj.children.all(), many=True).data
        return []

class NoteSerializer(serializers.ModelSerializer):
    class Meta:
        model = Note
        fields = ['id', 'title', 'content', 'created_at', 'updated_at']
        read_only_fields = ['id', 'created_at', 'updated_at']

    def create(self, validated_data):
        validated_data['user'] = self.context['request'].user
        return super().create(validated_data)

class CloudFileSerializer(serializers.ModelSerializer):
    children = serializers.SerializerMethodField()
    file_url = serializers.SerializerMethodField()
    formatted_size = serializers.SerializerMethodField()

    class Meta:
        model = CloudFile
        fields = ['id', 'name', 'file_type', 'parent', 'file_path', 'file_url', 'file_size', 'formatted_size', 'user', 'children', 'created_at', 'updated_at']
        read_only_fields = ['id', 'user', 'created_at', 'updated_at']

    def get_children(self, obj):
        if obj.file_type == 'folder' and obj.children.exists():
            return CloudFileSerializer(obj.children.all(), many=True).data
        return []

    def get_file_url(self, obj):
        if obj.file_path:
            request = self.context.get('request')
            if request:
                return request.build_absolute_uri(obj.file_path.url)
            return obj.file_path.url
        return None

    def get_formatted_size(self, obj):
        if obj.file_size < 1024:
            return f"{obj.file_size} B"
        elif obj.file_size < 1024 * 1024:
            return f"{obj.file_size / 1024:.1f} KB"
        elif obj.file_size < 1024 * 1024 * 1024:
            return f"{obj.file_size / (1024 * 1024):.1f} MB"
        else:
            return f"{obj.file_size / (1024 * 1024 * 1024):.1f} GB"

    def create(self, validated_data):
        validated_data['user'] = self.context['request'].user
        if 'file_path' in validated_data and validated_data['file_path']:
            validated_data['file_size'] = validated_data['file_path'].size
        return super().create(validated_data)