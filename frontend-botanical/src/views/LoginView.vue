<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">🌿</div>
        <h1 class="login-title">芳草空间</h1>
        <p class="login-subtitle">登录您的账户</p>
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
    <div class="background-decoration">
      <div class="leaf leaf-1">🍃</div>
      <div class="leaf leaf-2">🌸</div>
      <div class="leaf leaf-3">🌿</div>
      <div class="leaf leaf-4">🍂</div>
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
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--flora-base) 0%, var(--flora-base-dark) 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-card {
  background: var(--flora-base-light);
  padding: 3rem 2.5rem;
  border-radius: var(--radius-xl);
  box-shadow: 0 20px 60px var(--flora-shadow-dark);
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 10;
  animation: bloom-in 0.6s ease-out;
}

.login-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.logo {
  font-size: 3.5rem;
  margin-bottom: 1rem;
  display: inline-block;
  animation: wind-sway 4s ease-in-out infinite;
}

.login-title {
  font-family: var(--font-heading);
  color: var(--flora-stem);
  margin-bottom: 0.5rem;
  font-size: 2rem;
  letter-spacing: 0.05em;
}

.login-subtitle {
  font-family: var(--font-body);
  color: var(--flora-stem-light);
  font-size: 1rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-family: var(--font-body);
  color: var(--flora-stem);
  font-weight: 500;
  font-size: 0.95rem;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 1rem;
  font-size: 1.1rem;
  z-index: 1;
}

.form-group input {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 3rem;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 1rem;
  transition: all var(--transition-fast);
  background: white;
  color: #4a4543;
}

.form-group input:focus {
  outline: none;
  border-color: var(--flora-leaf);
  box-shadow: 0 0 0 4px rgba(123, 169, 56, 0.15);
}

.form-group input::placeholder {
  color: var(--flora-stem-light);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  background: linear-gradient(135deg, var(--flora-bloom-light), #fff);
  border: 1px solid var(--flora-bloom);
  border-radius: var(--radius-md);
  color: var(--flora-bloom-dark);
  font-size: 0.9rem;
  animation: shake 0.5s ease-in-out;
}

.error-icon {
  font-size: 1rem;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.login-btn {
  width: 100%;
  padding: 0.875rem;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(123, 169, 56, 0.35);
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
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--flora-base-dark);
}

.login-footer p {
  font-family: var(--font-body);
  color: var(--flora-stem-light);
  font-size: 0.95rem;
}

.register-link {
  color: var(--flora-leaf);
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition-fast);
}

.register-link:hover {
  color: var(--flora-leaf-dark);
  text-decoration: underline;
}

.background-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.leaf {
  position: absolute;
  font-size: 2rem;
  opacity: 0.15;
  animation: float 20s ease-in-out infinite;
}

.leaf-1 {
  top: 10%;
  left: 5%;
  animation-delay: 0s;
}

.leaf-2 {
  top: 60%;
  right: 10%;
  animation-delay: -5s;
}

.leaf-3 {
  bottom: 15%;
  left: 15%;
  animation-delay: -10s;
}

.leaf-4 {
  top: 30%;
  right: 5%;
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  25% {
    transform: translateY(-20px) rotate(5deg);
  }
  50% {
    transform: translateY(0) rotate(0deg);
  }
  75% {
    transform: translateY(20px) rotate(-5deg);
  }
}

@keyframes bloom-in {
  0% {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
    filter: blur(10px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
    filter: blur(0);
  }
}

@keyframes wind-sway {
  0%, 100% {
    transform: translateX(0) rotate(0deg);
  }
  25% {
    transform: translateX(3px) rotate(2deg);
  }
  75% {
    transform: translateX(-3px) rotate(-2deg);
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 2rem 1.5rem;
  }

  .logo {
    font-size: 2.5rem;
  }

  .login-title {
    font-size: 1.5rem;
  }
}
</style>