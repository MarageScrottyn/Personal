<template>
  <div class="video-form">
    <div class="form-header">
      <h2>{{ isEdit ? '编辑视频' : '新增视频' }}</h2>
      <router-link to="/admin/videos" class="back-btn">
        <span class="btn-icon">⬅️</span>
        <span>返回列表</span>
      </router-link>
    </div>
    <div class="form-content">
      <form @submit.prevent="handleSubmit">
        <div class="form-grid">
          <div class="form-group">
            <label for="title">视频标题</label>
            <input 
              type="text" 
              id="title" 
              v-model="formData.title" 
              required 
              placeholder="请输入视频标题"
            />
          </div>
          <div class="form-group">
            <label for="slug">Slug</label>
            <input 
              type="text" 
              id="slug" 
              v-model="formData.slug" 
              required 
              placeholder="请输入slug (小写字母和连字符)"
            />
          </div>
          <div class="form-group">
            <label for="duration">时长 (秒)</label>
            <input 
              type="number" 
              id="duration" 
              v-model="formData.duration" 
              required 
              min="1"
              placeholder="请输入视频时长"
            />
          </div>
          <div class="form-group">
            <label for="category">分类</label>
            <select id="category" v-model="formData.categories" required multiple>
              <option 
                v-for="category in categories" 
                :key="category.id" 
                :value="category.id"
              >
                {{ category.name }}
              </option>
            </select>
            <small>按住Ctrl键可选择多个分类</small>
          </div>
          <div class="form-group full-width">
            <label for="description">描述</label>
            <textarea 
              id="description" 
              v-model="formData.description" 
              required 
              placeholder="请输入视频描述" 
              rows="4"
            ></textarea>
          </div>
          <div class="form-group">
            <label>缩略图</label>
            <div class="image-upload">
              <div v-if="formData.thumbnail" class="image-preview">
                <img :src="getImageUrl(formData.thumbnail)" alt="缩略图预览" />
                <button type="button" @click="removeThumbnail" class="remove-image">
                  ✕
                </button>
              </div>
              <div v-else class="upload-area">
                <span class="upload-icon">📷</span>
                <p>点击或拖拽上传缩略图</p>
                <input 
                  type="file" 
                  accept="image/*" 
                  @change="handleThumbnailUpload"
                />
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>视频文件</label>
            <div class="file-upload">
              <div v-if="formData.video_file" class="file-preview">
                <span class="file-icon">🎥</span>
                <span class="file-name">{{ getFileName(formData.video_file) }}</span>
                <button type="button" @click="removeVideoFile" class="remove-file">
                  ✕
                </button>
              </div>
              <div v-else class="upload-area">
                <span class="upload-icon">🎬</span>
                <p>点击或拖拽上传视频文件</p>
                <input 
                  type="file" 
                  accept="video/*" 
                  @change="handleVideoUpload"
                />
              </div>
            </div>
          </div>
        </div>
        
        <div class="form-actions">
          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '../../../api/client'

const getImageUrl = (path) => {
  if (!path) return '/placeholder.png'
  if (path.startsWith('http')) {
    return path
  }
  if (path.startsWith('/media/')) {
    return path
  }
  if (path.startsWith('media/')) {
    return '/' + path
  }
  return '/media/' + path
}

const route = useRoute()
const router = useRouter()
const slug = route.params.slug
const isEdit = computed(() => !!slug)

const loading = ref(false)
const categories = ref([])
const formData = ref({
  title: '',
  slug: '',
  description: '',
  thumbnail: '',
  video_file: '',
  duration: '',
  categories: []
})

// 获取分类列表
const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/categories/')
    categories.value = response.data
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 获取视频详情（编辑模式）
const fetchVideoDetail = async () => {
  if (!isEdit.value) return
  
  try {
    loading.value = true
    const response = await apiClient.get(`/videos/${slug}/`)
    const video = response.data
    
    formData.value = {
      title: video.title,
      slug: video.slug,
      description: video.description,
      thumbnail: video.thumbnail,
      video_file: video.video_file,
      duration: video.duration,
      categories: video.categories || []
    }
  } catch (error) {
    console.error('获取视频详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理缩略图上传
const handleThumbnailUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    // 这里需要实现图片上传API
    // 暂时使用本地URL模拟
    formData.value.thumbnail = URL.createObjectURL(file)
  } catch (error) {
    console.error('上传缩略图失败:', error)
  }
}

// 移除缩略图
const removeThumbnail = () => {
  formData.value.thumbnail = ''
}

// 处理视频文件上传
const handleVideoUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    // 这里需要实现视频上传API
    // 暂时使用本地URL模拟
    formData.value.video_file = URL.createObjectURL(file)
  } catch (error) {
    console.error('上传视频失败:', error)
  }
}

// 移除视频文件
const removeVideoFile = () => {
  formData.value.video_file = ''
}

// 获取文件名
const getFileName = (url) => {
  if (!url) return ''
  return url.split('/').pop()
}

// 提交表单
const handleSubmit = async () => {
  try {
    loading.value = true
    
    // 确保categories是数字类型数组
    const data = {
      ...formData.value,
      categories: formData.value.categories.map(catId => parseInt(catId)),
      duration: parseInt(formData.value.duration)
    }
    
    if (isEdit.value) {
      await apiClient.put(`/admin/videos/${slug}/update/`, data)
    } else {
      await apiClient.post(`/admin/videos/create/`, data)
    }
    
    router.push('/admin/videos')
  } catch (error) {
    console.error('保存视频失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchCategories()
  if (isEdit.value) {
    await fetchVideoDetail()
  }
})
</script>

<style scoped>
.video-form {
  position: relative;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.form-header h2 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #f5f5f5;
  color: #333;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #e0e0e0;
}

.form-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #f093fb;
  box-shadow: 0 0 0 2px rgba(240, 147, 251, 0.1);
}

.image-upload,
.file-upload {
  margin-top: 8px;
}

.image-preview {
  position: relative;
  width: 200px;
  height: 150px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ddd;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-preview {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  width: 100%;
}

.file-icon {
  font-size: 24px;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-image,
.remove-file {
  position: absolute;
  top: 5px;
  right: 5px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  color: #e53935;
  transition: all 0.3s ease;
}

.remove-image:hover,
.remove-file:hover {
  background: white;
  transform: scale(1.1);
}

.upload-area {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-area:hover {
  border-color: #f093fb;
  background: rgba(240, 147, 251, 0.05);
}

.upload-area input {
  display: none;
}

.upload-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
}

.upload-area p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 30px;
}

.submit-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .form-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .form-content {
    padding: 20px;
  }
}
</style>