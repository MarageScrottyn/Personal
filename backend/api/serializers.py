from rest_framework import serializers
from django.conf import settings
from django.contrib.auth.models import User
from .models import Category, Comic, ComicChapter, Video, Profile, Note, CloudFile

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
        result = []
        for img_path in obj.images:
            img_path = str(img_path)
            if img_path.startswith('http://') or img_path.startswith('https://'):
                result.append(img_path)
            elif img_path.startswith('/media/'):
                result.append(img_path)
            elif img_path.startswith('media/'):
                result.append('/' + img_path)
            else:
                result.append('/media/' + img_path)
        return result

class ComicListSerializer(serializers.ModelSerializer):
    category_names = serializers.SerializerMethodField()
    categories = serializers.SerializerMethodField()
    cover_image = serializers.SerializerMethodField()

    class Meta:
        model = Comic
        fields = ['id', 'title', 'slug', 'description', 'cover_image', 'category_names', 'categories', 'author', 'created_at']

    def get_cover_image(self, obj):
        if obj.cover_image:
            cover_path = str(obj.cover_image)
            if cover_path.startswith('http://') or cover_path.startswith('https://'):
                return cover_path
            if cover_path.startswith('/media/'):
                return cover_path
            if cover_path.startswith('media/'):
                return '/' + cover_path
            return '/media/' + cover_path
        chapters = obj.chapters.all()
        if chapters:
            first_chapter = chapters.first()
            if first_chapter.images:
                for img_path in first_chapter.images:
                    if img_path:
                        img_path_str = str(img_path)
                        if img_path_str.startswith('http://') or img_path_str.startswith('https://'):
                            return img_path_str
                        if img_path_str.startswith('/media/'):
                            return img_path_str
                        if img_path_str.startswith('media/'):
                            return '/' + img_path_str
                        return '/media/' + img_path_str
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
        fields = ['id', 'title', 'slug', 'description', 'cover_image', 'category_names', 'categories', 'author', 'chapters', 'created_at', 'updated_at']

    def get_cover_image(self, obj):
        if obj.cover_image:
            cover_path = str(obj.cover_image)
            if cover_path.startswith('http://') or cover_path.startswith('https://'):
                return cover_path
            if cover_path.startswith('/media/'):
                return cover_path
            if cover_path.startswith('media/'):
                return '/' + cover_path
            return '/media/' + cover_path
        # 当没有封面图片时，使用章节图片中的第一章作为封面
        chapters = obj.chapters.all()
        if chapters:
            first_chapter = chapters.first()
            if first_chapter.images:
                for img_path in first_chapter.images:
                    if img_path:
                        img_path_str = str(img_path)
                        if img_path_str.startswith('http://') or img_path_str.startswith('https://'):
                            return img_path_str
                        if img_path_str.startswith('/media/'):
                            return img_path_str
                        if img_path_str.startswith('media/'):
                            return '/' + img_path_str
                        return '/media/' + img_path_str
        return ''

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

class ComicCreateUpdateSerializer(serializers.ModelSerializer):
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    cover_image = serializers.CharField(required=False, allow_blank=True, max_length=200)
    chapters = ComicChapterSerializer(many=True, required=False)

    class Meta:
        model = Comic
        fields = ['title', 'slug', 'description', 'cover_image', 'categories', 'author', 'chapters']

    def create(self, validated_data):
        chapters_data = validated_data.pop('chapters', [])
        categories = validated_data.pop('categories', [])

        # 处理封面图片路径
        cover_image_path = validated_data.pop('cover_image', None)
        if cover_image_path:
            if cover_image_path.startswith('/media/'):
                cover_image_path = cover_image_path[7:]
            elif cover_image_path.startswith('media/'):
                cover_image_path = cover_image_path[6:]
            validated_data['cover_image'] = cover_image_path

        comic = super().create(validated_data)
        
        # 设置分类
        if categories:
            comic.categories.set(categories)
        
        # 创建章节
        for chapter_data in chapters_data:
            ComicChapter.objects.create(comic=comic, **chapter_data)
        
        return comic

    def update(self, instance, validated_data):
        chapters_data = validated_data.pop('chapters', [])
        categories = validated_data.pop('categories', [])

        # 处理封面图片路径
        cover_image_path = validated_data.pop('cover_image', None)
        if cover_image_path:
            if cover_image_path.startswith('/media/'):
                cover_image_path = cover_image_path[7:]
            elif cover_image_path.startswith('media/'):
                cover_image_path = cover_image_path[6:]
            validated_data['cover_image'] = cover_image_path

        instance = super().update(instance, validated_data)
        
        # 设置分类
        if categories:
            instance.categories.set(categories)
        
        # 更新章节
        if chapters_data:
            # 删除旧章节
            instance.chapters.all().delete()
            # 创建新章节
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
        fields = ['id', 'title', 'slug', 'description', 'thumbnail', 'video_file', 'category_names', 'categories', 'created_at']

    def get_thumbnail(self, obj):
        if not obj.thumbnail:
            return ''
        thumb_path = str(obj.thumbnail)
        if thumb_path.startswith('http://') or thumb_path.startswith('https://'):
            return thumb_path
        if thumb_path.startswith('/media/'):
            return thumb_path
        if thumb_path.startswith('media/'):
            return '/' + thumb_path
        return '/media/' + thumb_path

    def get_video_file(self, obj):
        if not obj.video_file:
            return ''
        video_path = str(obj.video_file)
        if video_path.startswith('http://') or video_path.startswith('https://'):
            return video_path
        if video_path.startswith('/media/'):
            return video_path
        if video_path.startswith('media/'):
            return '/' + video_path
        return '/media/' + video_path

    def get_category_names(self, obj):
        return [category.name for category in obj.categories.all()]

    def get_categories(self, obj):
        return [category.id for category in obj.categories.all()]

class VideoCreateUpdateSerializer(serializers.ModelSerializer):
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    thumbnail = serializers.CharField(required=False, allow_blank=True, max_length=100)
    video_file = serializers.CharField(required=False, allow_blank=True, max_length=100)

    class Meta:
        model = Video
        fields = ['title', 'slug', 'description', 'thumbnail', 'video_file', 'categories']

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
        fields = ['id', 'title', 'slug', 'description', 'thumbnail', 'video_file', 'category_names', 'categories', 'duration', 'created_at', 'updated_at']

    def get_thumbnail(self, obj):
        if not obj.thumbnail:
            return ''
        thumb_path = str(obj.thumbnail)
        if thumb_path.startswith('http://') or thumb_path.startswith('https://'):
            return thumb_path
        if thumb_path.startswith('/media/'):
            return thumb_path
        if thumb_path.startswith('media/'):
            return '/' + thumb_path
        return '/media/' + thumb_path

    def get_video_file(self, obj):
        if not obj.video_file:
            return ''
        video_path = str(obj.video_file)
        if video_path.startswith('http://') or video_path.startswith('https://'):
            return video_path
        if video_path.startswith('/media/'):
            return video_path
        if video_path.startswith('media/'):
            return '/' + video_path
        return '/media/' + video_path

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