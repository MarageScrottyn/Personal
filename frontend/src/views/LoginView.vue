<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">🎬</div>
        <h1>媒体中心</h1>
        <p>登录您的账户</p>
      </div>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input
              id="username"
              v-model="username"
              type="text"
              placeholder="请输入用户名"
              required
              autocomplete="username"
            />
          </div>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              id="password"
              v-model="password"
              type="password"
              placeholder="请输入密码"
              required
              autocomplete="current-password"
            />
          </div>
        </div>
        <div v-if="error" class="error-message">
          <span class="error-icon">⚠️</span>
          {{ error }}
        </div>
        <button type="submit" class="login-btn" :disabled="loading">
          <span v-if="loading" class="loading-spinner"></span>
          <span v-else>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
      </form>
      <div class="login-footer">
        <p>还没有账号? <router-link to="/register" class="register-link">立即注册</router-link></p>
      </div>
    </div>
    <div class="background-shapes">
      <div class="shape shape1"></div>
      <div class="shape shape2"></div>
      <div class="shape shape3"></div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(username.value, password.value)
    router.push('/comics')
  } catch (e) {
    const errorData = e.response?.data
    if (errorData?.detail) {
      error.value = errorData.detail
    } else if (errorData?.error) {
      error.value = errorData.error
      if (errorData.detail) {
        error.value += ': ' + errorData.detail
      }
    } else {
      error.value = '登录失败，请检查网络连接或稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-card {
  background: white;
  padding: 40px 30px;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 400px;
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 50px;
  margin-bottom: 10px;
}

.login-header h1 {
  color: #2c3e50;
  margin-bottom: 5px;
  font-size: 24px;
}

.login-header p {
  color: #7f8c8d;
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 15px;
  font-size: 16px;
  z-index: 1;
}

.form-group input {
  width: 100%;
  padding: 14px 14px 14px 45px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.form-group input::placeholder {
  color: #b0b0b0;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fee;
  border-radius: 8px;
  color: #e74c3c;
  font-size: 14px;
  animation: shake 0.5s ease-in-out;
}

.error-icon {
  font-size: 16px;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.4);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  text-align: center;
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.login-footer p {
  color: #7f8c8d;
  font-size: 14px;
}

.register-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.register-link:hover {
  text-decoration: underline;
}

.background-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: hidden;
  z-index: 1;
}

.shape {
  position: absolute;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.shape1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  animation: float 6s ease-in-out infinite;
}

.shape2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: -50px;
  animation: float 8s ease-in-out infinite reverse;
}

.shape3 {
  width: 150px;
  height: 150px;
  top: 50%;
  left: 50%;
  animation: float 10s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
    border-radius: 15px;
  }

  .login-header h1 {
    font-size: 20px;
  }

  .form-group input {
    padding: 12px 12px 12px 40px;
    font-size: 14px;
  }

  .login-btn {
    padding: 12px;
    font-size: 15px;
  }
}
</style>