#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.serializers import VideoCreateUpdateSerializer
from api.models import Video

test_data = {
    'title': '测试视频',
    'slug': 'neiru_mechanical_age',
    'description': '测试描述',
    'thumbnail': '/media/videos/trailer.jpg',
    'video_file': '/media/videos/trailer.mp4',
    'categories': [1],
    'duration': 60
}

print("Testing VideoCreateUpdateSerializer with data:")
print(test_data)

try:
    video = Video.objects.get(slug='neiru_mechanical_age')
    print(f"\nFound video: {video.title}")

    serializer = VideoCreateUpdateSerializer(video, data=test_data, partial=True)

    if serializer.is_valid():
        print("\nSerializer is valid!")
        updated = serializer.save()
        print(f"Updated video: {updated.title}")
        print(f"Thumbnail: {updated.thumbnail}")
        print(f"Video file: {updated.video_file}")
    else:
        print("\nSerializer errors:")
        for field, errors in serializer.errors.items():
            print(f"  {field}: {errors}")
except Exception as e:
    print(f"\nException: {e}")
    import traceback
    traceback.print_exc()
