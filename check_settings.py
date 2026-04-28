import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from django.conf import settings
print(f"DEBUG: {settings.DEBUG}")
print(f"MEDIA_URL: {settings.MEDIA_URL}")
print(f"MEDIA_ROOT: {settings.MEDIA_ROOT}")