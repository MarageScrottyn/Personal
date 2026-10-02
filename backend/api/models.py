from django.db import models
from django.contrib.auth.models import User
import uuid

def video_upload_path(instance, filename):
    """根据视频类型返回不同的上传路径"""
    ext = filename.split('.')[-1]
    resource_id = instance.resource_id or str(uuid.uuid4())
    if instance.video_type == 'special':
        return f'special/files/{resource_id}.{ext}'
    return f'videos/files/{resource_id}.{ext}'

def video_thumbnail_path(instance, filename):
    """根据视频类型返回不同的缩略图路径"""
    ext = filename.split('.')[-1]
    resource_id = instance.resource_id or str(uuid.uuid4())
    if instance.video_type == 'special':
        return f'special/thumbnails/{resource_id}.{ext}'
    return f'videos/thumbnails/{resource_id}.{ext}'

def image_upload_path(instance, filename):
    """根据图片类型和所属图集返回不同的上传路径"""
    ext = filename.split('.')[-1]
    resource_id = instance.resource_id or str(uuid.uuid4())
    
    if instance.resource_type in ['album_image', 'album_cover'] and instance.album_id:
        if instance.resource_type == 'album_cover':
            return f'albums/{instance.album_id}/cover.{ext}'
        return f'albums/{instance.album_id}/{resource_id}.{ext}'
    
    return f'images/{resource_id}.{ext}'

def avatar_upload_path(instance, filename):
    """用户头像上传路径：按用户 ID 命名，同一用户更新头像即覆盖旧图"""
    ext = filename.split('.')[-1] if '.' in filename else 'jpg'
    return f'avatars/user_{instance.user.id}.{ext}'

class Profile(models.Model):
    USER_TYPE_CHOICES = (
        ('user', '普通用户'),
        ('admin', '管理员'),
    )
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='profile')
    user_type = models.CharField(max_length=10, choices=USER_TYPE_CHOICES, default='user')
    # 昵称：展示用名称，可为空，为空时前端回退显示登录账号
    nickname = models.CharField(max_length=30, blank=True, default='', verbose_name='昵称')
    # 头像：用户自行上传的头像图片
    avatar = models.ImageField(upload_to=avatar_upload_path, blank=True, null=True, verbose_name='头像')

    def __str__(self):
        return f"{self.user.username} - {self.get_user_type_display()}"

class Category(models.Model):
    name = models.CharField(max_length=100, verbose_name='标签名称')
    slug = models.SlugField(unique=True, verbose_name='标签标识')
    parent = models.ForeignKey('self', on_delete=models.SET_NULL, null=True, blank=True, related_name='children', verbose_name='父标签')
    permission_level = models.CharField(max_length=20, choices=[
        ('regular', '普通权限'),
        ('special', '特殊权限')
    ], default='regular', verbose_name='权限级别')
    
    class Meta:
        verbose_name = "标签"
        verbose_name_plural = "标签"
    
    def __str__(self):
        return self.name

class Image(models.Model):
    RESOURCE_TYPE_CHOICES = (
        ('image', '单张图片'),
        ('album_image', '图集图片'),
        ('album_cover', '图集封面'),
    )
    resource_id = models.CharField(max_length=36, null=True, blank=True, verbose_name='资源ID')
    image_file = models.ImageField(upload_to=image_upload_path, verbose_name='图片文件')
    album_id = models.CharField(max_length=36, blank=True, null=True, verbose_name='所属图集ID')
    title = models.CharField(max_length=200, blank=True, null=True, verbose_name='标题')
    description = models.TextField(blank=True, null=True, verbose_name='描述')
    categories = models.ManyToManyField(Category, related_name='images', blank=True, verbose_name='标签')
    uploader = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='uploaded_images', verbose_name='上传者')
    resource_type = models.CharField(max_length=20, choices=RESOURCE_TYPE_CHOICES, default='image', verbose_name='资源类型')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')
    
    def save(self, *args, **kwargs):
        if not self.resource_id:
            self.resource_id = str(uuid.uuid4())
        super().save(*args, **kwargs)
    
    def __str__(self):
        return self.title or f"Image {self.resource_id[:8]}"

class Album(models.Model):
    resource_id = models.CharField(max_length=36, null=True, blank=True, verbose_name='资源ID')
    title = models.CharField(max_length=200, verbose_name='标题')
    description = models.TextField(blank=True, null=True, verbose_name='描述')
    cover_image = models.ForeignKey(Image, on_delete=models.SET_NULL, null=True, blank=True, related_name='album_cover', verbose_name='封面图片')
    images = models.ManyToManyField(Image, related_name='albums', blank=True, verbose_name='图集图片')
    categories = models.ManyToManyField(Category, related_name='albums', blank=True, verbose_name='标签')
    author = models.CharField(max_length=100, blank=True, null=True, verbose_name='作者')
    uploader = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='uploaded_albums', verbose_name='上传者')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')
    
    def save(self, *args, **kwargs):
        if not self.resource_id:
            self.resource_id = str(uuid.uuid4())
        super().save(*args, **kwargs)
    
    def get_ordered_images(self):
        """获取按排序序号排列的图片列表"""
        return [entry.image for entry in self.album_images.select_related('image').order_by('order')]
    
    def __str__(self):
        return self.title

class AlbumImage(models.Model):
    """图集图片中间模型 - 支持图片排序"""
    album = models.ForeignKey(Album, on_delete=models.CASCADE, related_name='album_images', verbose_name='图集')
    image = models.ForeignKey(Image, on_delete=models.CASCADE, related_name='album_image_entries', verbose_name='图片')
    order = models.IntegerField(default=0, verbose_name='排序序号')
    
    class Meta:
        ordering = ['order']
        verbose_name = '图集图片'
        verbose_name_plural = '图集图片'
        unique_together = ('album', 'image')
    
    def __str__(self):
        return f"{self.album} - {self.image} (序号:{self.order})"

class Comic(models.Model):
    resource_id = models.CharField(max_length=36, null=True, blank=True, verbose_name='资源ID')
    title = models.CharField(max_length=200, verbose_name='标题')
    slug = models.SlugField(unique=True, blank=True, null=True, verbose_name='标识')
    description = models.TextField(verbose_name='描述')
    cover_image = models.ImageField(upload_to='comics/covers/', null=True, blank=True, verbose_name='封面图片')
    categories = models.ManyToManyField(Category, related_name='comics', verbose_name='标签')
    author = models.CharField(max_length=100, verbose_name='作者')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')
    
    def save(self, *args, **kwargs):
        if not self.resource_id:
            self.resource_id = str(uuid.uuid4())
        super().save(*args, **kwargs)
    
    def __str__(self):
        return self.title

class ComicChapter(models.Model):
    comic = models.ForeignKey(Comic, on_delete=models.CASCADE, related_name='chapters')
    title = models.CharField(max_length=200)
    chapter_number = models.IntegerField()
    images = models.JSONField(default=list, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    
    def __str__(self):
        return f"{self.comic.title} - {self.title}"

class Video(models.Model):
    VIDEO_TYPE_CHOICES = (
        ('video', '视频'),
        ('special', '特辑'),
    )
    resource_id = models.CharField(max_length=36, null=True, blank=True, verbose_name='资源ID')
    title = models.CharField(max_length=200, verbose_name='标题')
    slug = models.SlugField(unique=True, blank=True, null=True, verbose_name='标识')
    description = models.TextField(blank=True, null=True, verbose_name='描述')
    thumbnail = models.ImageField(upload_to=video_thumbnail_path, null=True, blank=True, verbose_name='缩略图')
    video_file = models.FileField(upload_to=video_upload_path, null=True, blank=True, verbose_name='视频文件')
    m3u8_path = models.CharField(max_length=500, blank=True, null=True, verbose_name='M3U8路径')
    categories = models.ManyToManyField(Category, related_name='videos', verbose_name='标签')
    duration = models.IntegerField(help_text="时长（秒）", default=0, null=True, blank=True, verbose_name='时长')
    video_type = models.CharField(max_length=20, choices=VIDEO_TYPE_CHOICES, default='video', verbose_name='视频类型')
    tags = models.JSONField(default=list, blank=True, verbose_name='附加标签')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    def save(self, *args, **kwargs):
        if not self.resource_id:
            self.resource_id = str(uuid.uuid4())
        super().save(*args, **kwargs)
    
    def __str__(self):
        return self.title

class Note(models.Model):
    title = models.CharField(max_length=200)
    content = models.TextField(blank=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='notes')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.title

class CloudFile(models.Model):
    FILE_TYPE_CHOICES = (
        ('folder', '文件夹'),
        ('file', '文件'),
    )
    name = models.CharField(max_length=255)
    file_type = models.CharField(max_length=10, choices=FILE_TYPE_CHOICES, default='file')
    parent = models.ForeignKey('self', on_delete=models.CASCADE, null=True, blank=True, related_name='children')
    file_path = models.FileField(upload_to='cloud/', null=True, blank=True)
    file_size = models.BigIntegerField(default=0)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='cloud_files')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return self.name