import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Video
from api.serializers import VideoDetailSerializer

videos = Video.objects.all()
for v in videos:
    print(f"\n=== {v.title} ===")
    print(f"Slug: {v.slug}")
    print(f"Video file path: {v.video_file}")
    print(f"Video file url: {v.video_file.url if v.video_file else 'None'}")