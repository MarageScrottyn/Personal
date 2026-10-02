// 检查localStorage中的token
console.log('Access Token:', localStorage.getItem('accessToken'));
console.log('Refresh Token:', localStorage.getItem('refreshToken'));
console.log('Token present:', !!localStorage.getItem('accessToken'));

// 测试API调用
if (localStorage.getItem('accessToken')) {
  fetch('/api/categories/', {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  })
  .then(response => response.json())
  .then(data => console.log('Categories:', data))
  .catch(error => console.error('API error:', error));
} else {
  console.log('No token found');
  // 尝试登录
  fetch('/api/auth/login/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: 'testuser',
      password: 'test123'
    })
  })
  .then(response => response.json())
  .then(data => {
    console.log('Login response:', data);
    if (data.access) {
      localStorage.setItem('accessToken', data.access);
      localStorage.setItem('refreshToken', data.refresh);
      console.log('Token saved to localStorage');
    }
  })
  .catch(error => console.error('Login error:', error));
}