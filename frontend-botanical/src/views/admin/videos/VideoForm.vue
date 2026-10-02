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
              <div 
                v-else 
                class="upload-area"
                @dragover.prevent
                @dragenter.prevent
                @drop="handleThumbnailDrop"
                @click="triggerThumbnailUpload"
              >
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
              <div 
                v-else 
                class="upload-area"
                @dragover.prevent
                @dragenter.prevent
                @drop="handleVideoDrop"
                @click="triggerVideoUpload"
              >
                <span class="upload-icon">🎬</span>
                <p>点击或拖拽上传视频文件或文件夹</p>
                <p class="upload-hint">支持：.m3u8, .mp4, .webm, .mkv, .mov, .avi 等格式</p>
                <input 
                  type="file" 
                  accept="video/*,.m3u8,.mp4,.webm,.mkv,.mov,.avi,.flv,.wmv,.ts" 
                  @change="handleVideoUpload"
                  webkitdirectory
                  directory
                  multiple
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
    loading.value = true
    const uploadFormData = new FormData()
    uploadFormData.append('file', file)
    
    const response = await apiClient.post('/upload/video-thumbnail/', uploadFormData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    formData.value.thumbnail = response.data.path
  } catch (error) {
    console.error('上传缩略图失败:', error)
    alert('上传缩略图失败，请重试')
  } finally {
    loading.value = false
  }
}

// 移除缩略图
const removeThumbnail = () => {
  formData.value.thumbnail = ''
}

// 处理视频文件上传
const handleVideoUpload = async (event) => {
  const files = Array.from(event.target.files || [])
  if (!files || files.length === 0) return
  
  try {
    loading.value = true
    
    const m3u8File = files.find(f => f.name.endsWith('.m3u8'))
    
    if (m3u8File && files.length > 1) {
      // 处理m3u8文件夹上传
      const uploadFormData = new FormData()
      
      files.forEach(file => {
        uploadFormData.append('files', file)
      })
      
      const response = await apiClient.post('/upload/video-folder/', uploadFormData)
      
      formData.value.video_file = response.data.m3u8_path
      if (response.data.thumbnail) {
        formData.value.thumbnail = response.data.thumbnail
      }
      alert('m3u8视频文件夹上传成功！')
    } else {
      // 处理单个文件上传
      const file = files[0]
      const uploadFormData = new FormData()
      uploadFormData.append('file', file)
      
      const response = await apiClient.post('/upload/video/', uploadFormData)
      
      formData.value.video_file = response.data.path
      if (response.data.thumbnail) {
        formData.value.thumbnail = response.data.thumbnail
      }
    }
  } catch (error) {
    console.error('上传视频失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    alert(`上传视频失败: ${error.response?.data?.error || error.message}`)
  } finally {
    loading.value = false
  }
}

// 移除视频文件
const removeVideoFile = () => {
  formData.value.video_file = ''
}

// 触发缩略图文件选择
const triggerThumbnailUpload = (event) => {
  event.stopPropagation()
  const container = event.currentTarget
  const input = container.querySelector('input[type="file"]')
  if (input) {
    input.click()
  }
}

// 触发视频文件选择
const triggerVideoUpload = (event) => {
  event.stopPropagation()
  const container = event.currentTarget
  const input = container.querySelector('input[type="file"]')
  if (input) {
    input.click()
  }
}

// 处理缩略图拖拽上传
const handleThumbnailDrop = (event) => {
  event.preventDefault()
  event.stopPropagation()
  const file = event.dataTransfer.files[0]
  if (file && file.type.startsWith('image/')) {
    const container = event.currentTarget
    const input = container.querySelector('input[type="file"]')
    if (input) {
      const dataTransfer = new DataTransfer()
      dataTransfer.items.add(file)
      input.files = dataTransfer.files
      handleThumbnailUpload({ target: input })
    }
  }
}

// 处理视频拖拽上传
const handleVideoDrop = (event) => {
  event.preventDefault()
  event.stopPropagation()
  const file = event.dataTransfer.files[0]
  if (file && file.type.startsWith('video/')) {
    const container = event.currentTarget
    const input = container.querySelector('input[type="file"]')
    if (input) {
      const dataTransfer = new DataTransfer()
      dataTransfer.items.add(file)
      input.files = dataTransfer.files
      handleVideoUpload({ target: input })
    }
  }
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
      categories: formData.value.categories.map(catId => parseInt(catId))
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
  font-family: var(--font-heading);
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--flora-base);
  color: var(--flora-stem);
  text-decoration: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: var(--flora-base-dark);
}

.form-content {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 30px;
  box-shadow: 0 4px 15px var(--flora-shadow);
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
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--flora-stem);
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 12px;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: border-color var(--transition-fast);
  background: var(--flora-base);
  color: var(--flora-stem);
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--flora-bloom);
}

.image-upload,
.file-upload {
  margin-top: 8px;
}

.image-preview {
  position: relative;
  width: 200px;
  height: 150px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--flora-base-dark);
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
  background: var(--flora-base);
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  width: 100%;
}

.file-icon {
  font-size: 1.5rem;
}

.file-name {
  flex: 1;
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem);
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
  font-size: 1rem;
  color: var(--flora-bloom-dark);
  transition: all var(--transition-fast);
}

.remove-image:hover,
.remove-file:hover {
  background: white;
  transform: scale(1.1);
}

.upload-area {
  border: 2px dashed var(--flora-base-dark);
  border-radius: var(--radius-md);
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.upload-area:hover {
  border-color: var(--flora-bloom);
  background: rgba(0, 0, 0, 0.02);
}

.upload-area input {
  display: none;
}

.upload-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 15px;
}

.upload-area p {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
  margin: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 30px;
}

.submit-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--flora-bloom) 0%, var(--flora-bloom-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
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