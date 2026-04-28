#!/usr/bin/env python3
import os
import sys
import django

sys.path.insert(0, '/data/data/com.termux/files/home/Personal/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')
django.setup()

from django.contrib.auth.models import User

print("=" * 60)
print("检查用户")
print("=" * 60)

# 查找包含 marage 的用户
users = User.objects.filter(username__icontains='marage')
print(f"\n找到包含'marage'的用户: {users.count()}")
for u in users:
    print(f"  - {u.username} (active: {u.is_active}, is_staff: {u.is_staff})")

# 检查所有用户
all_users = User.objects.all()
print(f"\n所有用户: {all_users.count()}")
for u in all_users:
    print(f"  - {u.username} (active: {u.is_active})")
