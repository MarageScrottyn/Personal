import os
import sys
import django

# 设置Django环境
sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Video

videos = Video.objects.all()
for v in videos:
    print(f"Title: {v.title}, Slug: {v.slug}, Video File: {v.video_file}")