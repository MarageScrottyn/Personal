#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Category, Comic, Video
from api.serializers import ComicListSerializer, VideoListSerializer
from rest_framework.request import Request
from django.test import RequestFactory

print("=" * 60)
print("权限过滤测试")
print("=" * 60)

# 获取所有分类
all_categories = Category.objects.all()
print(f"\n所有分类 ({all_categories.count()}):")
for cat in all_categories:
    print(f"  - {cat.name} (slug: {cat.slug}, permission: {cat.permission_level})")

# 测试普通用户视角
regular_categories = Category.objects.filter(permission_level='regular')
print(f"\n普通权限分类 ({regular_categories.count()}):")
for cat in regular_categories:
    print(f"  - {cat.name}")

special_categories = Category.objects.filter(permission_level='special')
print(f"\n特殊权限分类 ({special_categories.count()}):")
for cat in special_categories:
    print(f"  - {cat.name}")

# 测试漫画列表
comics = Comic.objects.all()
print(f"\n所有漫画 ({comics.count()}):")
for comic in comics:
    cat_names = [c.name for c in comic.categories.all()]
    print(f"  - {comic.title} (分类: {', '.join(cat_names)})")

# 测试普通用户可见的漫画
regular_comics = Comic.objects.filter(categories__in=regular_categories).distinct()
print(f"\n普通用户可见的漫画 ({regular_comics.count()}):")
for comic in regular_comics:
    cat_names = [c.name for c in comic.categories.all()]
    print(f"  - {comic.title} (分类: {', '.join(cat_names)})")

# 测试视频列表
videos = Video.objects.all()
print(f"\n所有视频 ({videos.count()}):")
for video in videos:
    cat_names = [c.name for c in video.categories.all()]
    print(f"  - {video.title} (分类: {', '.join(cat_names)})")

# 测试普通用户可见的视频
regular_videos = Video.objects.filter(categories__in=regular_categories).distinct()
print(f"\n普通用户可见的视频 ({regular_videos.count()}):")
for video in regular_videos:
    cat_names = [c.name for c in video.categories.all()]
    print(f"  - {video.title} (分类: {', '.join(cat_names)})")
