#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from django.contrib.auth.models import User
from api.models import Profile

print("=" * 60)
print("检查用户Profile")
print("=" * 60)

users = User.objects.filter(username__icontains='marage')
for u in users:
    try:
        profile = u.profile
        print(f"用户: {u.username}")
        print(f"  - user_type: {profile.user_type}")
        print(f"  - is_admin: {profile.user_type == 'admin'}")
    except Profile.DoesNotExist:
        print(f"用户: {u.username} - 没有Profile")

# 测试登录
print("\n" + "=" * 60)
print("测试登录")
print("=" * 60)

for u in users:
    print(f"\n尝试登录用户: {u.username}")
    # 这里不测试密码，只是检查账户状态
    print(f"  - is_active: {u.is_active}")
    print(f"  - is_staff: {u.is_staff}")
