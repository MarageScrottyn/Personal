#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from api.models import Profile
from django.contrib.auth.models import User

print("Users and their profiles:")
for user in User.objects.all():
    if hasattr(user, 'profile'):
        print(f"- {user.username}: {user.profile.user_type}")
    else:
        print(f"- {user.username}: no profile")
