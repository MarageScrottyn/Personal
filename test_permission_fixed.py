#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Category, Comic, Video

print("=" * 60)
print("权限过滤测试（修复后）")
print("=" * 60)

# 获取所有分类
all_categories = Category.objects.all()
print(f"\n所有分类 ({all_categories.count()}):")
for cat in all_categories:
    print(f"  - {cat.name} (slug: {cat.slug}, permission: {cat.permission_level})")

# 获取特殊权限分类
special_categories = Category.objects.filter(permission_level='special')
print(f"\n特殊权限分类 ({special_categories.count()}):")
for cat in special_categories:
    print(f"  - {cat.name}")

# 测试普通用户不可见的漫画（使用exclude，排除包含特殊权限分类的漫画）
hidden_comics = Comic.objects.filter(categories__in=special_categories).distinct()
print(f"\n普通用户不可见的漫画（包含特殊权限分类）({hidden_comics.count()}):")
for comic in hidden_comics:
    cat_names = [c.name for c in comic.categories.all()]
    print(f"  - {comic.title} (分类: {', '.join(cat_names)})")

# 测试普通用户可见的漫画（使用exclude，排除包含特殊权限分类的漫画）
visible_comics = Comic.objects.exclude(categories__in=special_categories).distinct()
print(f"\n普通用户可见的漫画（所有分类都是普通权限）({visible_comics.count()}):")
for comic in visible_comics:
    cat_names = [c.name for c in comic.categories.all()]
    print(f"  - {comic.title} (分类: {', '.join(cat_names)})")

# 测试普通用户不可见的视频
hidden_videos = Video.objects.filter(categories__in=special_categories).distinct()
print(f"\n普通用户不可见的视频（包含特殊权限分类）({hidden_videos.count()}):")
for video in hidden_videos:
    cat_names = [c.name for c in video.categories.all()]
    print(f"  - {video.title} (分类: {', '.join(cat_names)})")

# 测试普通用户可见的视频
visible_videos = Video.objects.exclude(categories__in=special_categories).distinct()
print(f"\n普通用户可见的视频（所有分类都是普通权限）({visible_videos.count()}):")
for video in visible_videos:
    cat_names = [c.name for c in video.categories.all()]
    print(f"  - {video.title} (分类: {', '.join(cat_names)})")
