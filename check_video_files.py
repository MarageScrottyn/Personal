#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Video

v = Video.objects.get(slug='neiru_mechanical_age')
print(f"Video: {v.title}")
print(f"thumbnail: {v.thumbnail}")
print(f"video_file: {v.video_file}")
