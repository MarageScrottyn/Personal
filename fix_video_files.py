#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Video
from django.core.files.base import ContentFile
import os

MEDIA_ROOT = '/data/data/com.termux/files/home/Personal/backend/media'

videos_dir = os.path.join(MEDIA_ROOT, 'videos')

videos = Video.objects.all()
for v in videos:
    print(f"Video: {v.title} (slug: {v.slug})")
    print(f"  Current thumbnail: {v.thumbnail}")
    print(f"  Current video_file: {v.video_file}")

    if not v.thumbnail:
        jpg_files = [f for f in os.listdir(videos_dir) if f.endswith('.jpg')]
        if jpg_files:
            thumb_path = os.path.join(videos_dir, jpg_files[0])
            with open(thumb_path, 'rb') as f:
                v.thumbnail.save(jpg_files[0], ContentFile(f.read()), save=True)
            print(f"  Updated thumbnail to: {jpg_files[0]}")

    if not v.video_file:
        mp4_files = [f for f in os.listdir(videos_dir) if f.endswith('.mp4') and not f.startswith('oceans') and not f.startswith('trailer')]
        if mp4_files:
            vid_path = os.path.join(videos_dir, mp4_files[0])
            with open(vid_path, 'rb') as f:
                v.video_file.save(mp4_files[0], ContentFile(f.read()), save=True)
            print(f"  Updated video_file to: {mp4_files[0]}")

    print()
