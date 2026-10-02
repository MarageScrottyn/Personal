import os, sys
sys.path.insert(0, os.path.dirname(__file__))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'media_site.settings')

import django
django.setup()

from rest_framework.test import APIRequestFactory, APIClient
from api.views import ComicListView
from api.models import User

factory = APIRequestFactory()
client = APIClient()

user = User.objects.first()
print(f'测试用户: {user}')

client.force_authenticate(user=user)

response = client.get('/api/comics/')
print(f'API响应状态码: {response.status_code}')
print(f'API响应数据: {response.json()}')

print('\n=== 直接数据库查询 ===')
from api.models import Comic
for c in Comic.objects.all():
    print(f'{c.title}, categories: {list(c.categories.all())}')
