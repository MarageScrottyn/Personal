<template>
  <div class="comic-form">
    <div class="form-header">
      <h1>{{ isEdit ? '编辑漫画' : '创建漫画' }}</h1>
      <button @click="goBack" class="back-btn">
        ← 返回漫画管理
      </button>
    </div>
    
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <form @submit.prevent="handleSubmit" class="form-content">
      <div class="form-group">
        <label for="title">标题</label>
        <input 
          type="text" 
          id="title" 
          v-model="formData.title" 
          required
        />
      </div>
      
      <div class="form-group">
        <label for="slug">别名</label>
        <input 
          type="text" 
          id="slug" 
          v-model="formData.slug" 
          required
        />
      </div>
      
      <div class="form-group">
        <label for="description">描述</label>
        <textarea 
          id="description" 
          v-model="formData.description" 
          rows="4"
        ></textarea>
      </div>
      
      <div class="form-group">
        <label for="author">作者</label>
        <input 
          type="text" 
          id="author" 
          v-model="formData.author" 
          required
        />
      </div>
      
      <div class="form-group">
        <label for="category">分类</label>
        <select 
          id="category" 
          v-model="formData.categories" 
          required
          multiple
        >
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
      
      <div class="form-group">
        <label>封面图片</label>
        <div class="cover-upload">
          <input 
            type="file" 
            id="cover-image-upload" 
            ref="coverImageInput"
            @change="handleCoverImageUpload"
            accept="image/*"
            style="display: none;"
          />
          <label for="cover-image-upload" class="cover-upload-label">
            <img 
              :src="getImageUrl(formData.cover_image || (chapters.length > 0 && chapters[0].images && chapters[0].images.length > 0 ? chapters[0].images[0] : ''))" 
              alt="封面预览" 
              class="cover-image"
            />
            <span class="upload-icon">📷</span>
            <span class="upload-text">{{ formData.cover_image ? '更换图片' : '上传封面' }}</span>
          </label>
        </div>
      </div>
      
      <div class="chapters-section">
        <h3>章节管理</h3>
        
        <div v-for="(chapter, index) in chapters" :key="index" class="chapter-item">
          <div class="chapter-header">
            <div class="chapter-info">
              <div class="form-group">
                <label :for="`chapter-title-${index}`">章节标题</label>
                <input 
                  type="text" 
                  :id="`chapter-title-${index}`" 
                  v-model="chapter.title" 
                  required
                />
              </div>
              
              <div class="form-group">
                <label :for="`chapter-number-${index}`">章节号</label>
                <input 
                  type="number" 
                  :id="`chapter-number-${index}`" 
                  v-model.number="chapter.chapter_number" 
                  required
                />
              </div>
            </div>
            
            <button 
              type="button" 
              @click="removeChapter(index)" 
              class="remove-btn"
            >
              删除章节
            </button>
          </div>
          
          <div class="images-section">
            <h4>章节图片</h4>
            <div class="images-preview" 
                 @dragover.prevent
                 @drop="handleChapterImageReorder(index, $event)">
              <div v-for="(image, imgIndex) in getSortedImages(chapter.images)" 
                   :key="imgIndex" 
                   class="image-item"
                   draggable="true"
                   @dragstart="(e) => handleDragStart(e, imgIndex)"
                   @dragend="handleDragEnd"
                   @dragover.prevent="(e) => handleDragOver(e, imgIndex)"
                   @drop="(e) => handleDrop(e, index, imgIndex)"
                   :class="{ 'dragging': draggedIndex === imgIndex, 'drag-over': dragOverIndex === imgIndex }">
                <span class="drag-handle">⋮⋮</span>
                <img :src="getImageUrl(image)" alt="章节图片" />
                <span class="image-order">{{ imgIndex + 1 }}</span>
                <button type="button" @click="removeChapterImage(index, imgIndex)" class="remove-image">
                  ✕
                </button>
              </div>
              <div class="image-upload-item">
                <input 
                  type="file" 
                  :id="`chapter-image-upload-${index}`"
                  @change="(e) => handleChapterImageUpload(index, e)"
                  accept="image/*"
                  multiple
                  style="display: none;"
                />
                <label :for="`chapter-image-upload-${index}`" class="image-upload-label">
                  <span class="upload-icon">➕</span>
                  <span>添加图片</span>
                </label>
              </div>
            </div>
          </div>
        </div>
        
        <button type="button" @click="addChapter" class="add-chapter-btn">
          + 添加章节
        </button>
      </div>
      
      <div class="form-actions">
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '保存中...' : '保存漫画' }}
        </button>
        <button type="button" @click="goBack" class="cancel-btn">
          取消
        </button>
      </div>
    </form>
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
  return `/media/${path}`
}

const route = useRoute()
const router = useRouter()
const slug = route.params.slug
const isEdit = computed(() => !!slug)

const loading = ref(false)
const draggedIndex = ref(null)
const dragOverIndex = ref(null)
const categories = ref([])
const coverImageInput = ref(null)
const formData = ref({
  title: '',
  slug: '',
  description: '',
  author: '',
  categories: [],
  cover_image: ''
})

const chapters = ref([])

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/categories/')
    categories.value = response.data
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchComicDetail = async () => {
  if (!slug) return // 如果没有slug，直接返回
  
  try {
    const response = await apiClient.get(`/comics/${slug}/`)
    const comic = response.data
    formData.value = {
      title: comic.title,
      slug: comic.slug,
      description: comic.description,
      author: comic.author,
      categories: comic.categories || [],
      cover_image: comic.cover_image
    }
    chapters.value = comic.chapters || []
  } catch (error) {
    console.error('获取漫画详情失败:', error)
  }
}

const addChapter = () => {
  chapters.value.push({
    title: '',
    chapter_number: chapters.value.length + 1,
    images: []
  })
}

const removeChapter = (index) => {
  chapters.value.splice(index, 1)
  // 更新章节号
  chapters.value.forEach((chapter, i) => {
    chapter.chapter_number = i + 1
  })
}

const removeChapterImage = (chapterIndex, imgIndex) => {
  chapters.value[chapterIndex].images.splice(imgIndex, 1)
}

const handleDragStart = (event, index) => {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', index)
  draggedIndex.value = index
  console.log('Drag started:', index)
}

const handleDragEnd = () => {
  console.log('Drag ended')
  draggedIndex.value = null
  dragOverIndex.value = null
}

const handleDragOver = (event, index) => {
  event.preventDefault()
  event.dataTransfer.dropEffect = 'move'
  dragOverIndex.value = index
  console.log('Drag over:', index)
}

const handleDrop = (event, chapterIndex, targetIndex) => {
  event.preventDefault()
  const sourceIndex = parseInt(event.dataTransfer.getData('text/plain'))
  console.log('Drop event:', { chapterIndex, sourceIndex, targetIndex })
  
  if (sourceIndex !== targetIndex) {
    const chapter = chapters.value[chapterIndex]
    const images = [...chapter.images]
    const [draggedImage] = images.splice(sourceIndex, 1)
    images.splice(targetIndex, 0, draggedImage)
    chapter.images = images
    console.log('Images reordered:', images)
  }
  
  draggedIndex.value = null
  dragOverIndex.value = null
}

const handleChapterImageReorder = (chapterIndex, event) => {
  event.preventDefault()
  console.log('Drop event on container:', { chapterIndex, draggedIndex: draggedIndex.value, dragOverIndex: dragOverIndex.value })
  if (draggedIndex.value !== null && dragOverIndex.value !== null) {
    const chapter = chapters.value[chapterIndex]
    const images = [...chapter.images]
    const [draggedImage] = images.splice(draggedIndex.value, 1)
    images.splice(dragOverIndex.value, 0, draggedImage)
    chapter.images = images
    console.log('Images reordered:', images)
  }
  draggedIndex.value = null
  dragOverIndex.value = null
}

const getSortedImages = (images) => {
  return images || []
}

const handleCoverImageUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  try {
    const uploadData = new FormData()
    uploadData.append('file', file)
    uploadData.append('comic_slug', formData.value.slug)
    uploadData.append('is_cover', true)

    const response = await apiClient.post('/upload/image/', uploadData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    let path = response.data.path
    formData.value.cover_image = path
  } catch (error) {
    console.error('上传封面图片失败:', error)
  }
}

const handleChapterImageUpload = async (chapterIndex, event) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  try {
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const uploadData = new FormData()
      uploadData.append('file', file)
      uploadData.append('comic_slug', formData.value.slug)
      uploadData.append('chapter_index', chapterIndex)

      const response = await apiClient.post('/upload/image/', uploadData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      // 确保路径格式正确
      let path = response.data.path

      if (!chapters.value[chapterIndex].images) {
        chapters.value[chapterIndex].images = []
      }
      chapters.value[chapterIndex].images.push(path)
    }
  } catch (error) {
    console.error('上传章节图片失败:', error)
  }
}

const handleSubmit = async () => {
  try {
    loading.value = true
    
    // 当没有上传封面时，使用第一章的第一张图片作为封面
    let finalCoverImage = formData.value.cover_image
    if (!finalCoverImage && chapters.value.length > 0 && chapters.value[0].images && chapters.value[0].images.length > 0) {
      finalCoverImage = chapters.value[0].images[0]
    }
    
    // 确保categories是数字类型数组
    const data = {
      ...formData.value,
      cover_image: finalCoverImage,
      categories: formData.value.categories.map(catId => parseInt(catId)),
      chapters: chapters.value.map(chapter => ({
        ...chapter,
        chapter_number: chapter.chapter_number ? parseInt(chapter.chapter_number) : 1
      }))
    }
    
    console.log('发送的数据:', data)
    console.log('Categories type:', typeof data.categories)
    console.log('Chapter number type:', typeof data.chapters[0]?.chapter_number)
    
    // 检查localStorage中的token
    console.log('Access Token:', localStorage.getItem('accessToken'))
    console.log('Token present:', !!localStorage.getItem('accessToken'))
    
    if (isEdit.value) {
      console.log('Update URL:', `/admin/comics/${slug}/update/`)
      const response = await apiClient.put(`/admin/comics/${slug}/update/`, data)
      console.log('Update response:', response.data)
    } else {
      console.log('Create URL:', `/admin/comics/create/`)
      const response = await apiClient.post(`/admin/comics/create/`, data)
      console.log('Create response:', response.data)
    }
    
    router.push('/admin/comics')
  } catch (error) {
    console.error('保存漫画失败:', error)
    console.error('错误详情:', error.response?.data)
    console.error('Error status:', error.response?.status)
    console.error('Error headers:', error.response?.headers)
  } finally {
    loading.value = false
  }
}

const goBack = async () => {
  // 如果是新建漫画且已上传文件，清理临时文件
  if (!isEdit.value && formData.value.slug) {
    try {
      await apiClient.post('/cleanup-temp/', {
        comic_slug: formData.value.slug
      })
    } catch (error) {
      console.error('清理临时文件失败:', error)
    }
  }
  router.push('/admin/comics')
}

onMounted(async () => {
  await fetchCategories()
  if (isEdit.value) {
    await fetchComicDetail()
  } else {
    addChapter()
  }
})
</script>

<style scoped>
.comic-form {
  padding: 20px 0;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.form-header h1 {
  font-size: 24px;
  color: #2c3e50;
  margin: 0;
}

.back-btn {
  background: #f5f5f5;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s ease;
}

.back-btn:hover {
  background: #e0e0e0;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 15px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.form-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #34495e;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.cover-upload {
  margin-top: 10px;
}

.cover-upload-label {
  display: inline-block;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cover-upload-label:hover {
  transform: translateY(-2px);
}

.cover-upload-label .cover-image {
  width: 150px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.cover-upload-label:hover .cover-image {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.cover-upload-label .upload-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 24px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(0, 0, 0, 0.5);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.cover-upload-label:hover .upload-icon {
  opacity: 1;
}

.cover-upload-label .upload-text {
  position: absolute;
  bottom: 10px;
  left: 0;
  right: 0;
  text-align: center;
  color: white;
  background: rgba(0, 0, 0, 0.7);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0;
  transition: all 0.3s ease;
}

.cover-upload-label:hover .upload-text {
  opacity: 1;
}

.chapters-section {
  margin-top: 40px;
}

.chapters-section h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
}

.chapter-item {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #e0e0e0;
}

.chapter-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.chapter-info {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.remove-btn {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s ease;
}

.remove-btn:hover {
  background: #c0392b;
}

.images-section h4 {
  font-size: 16px;
  color: #34495e;
  margin-bottom: 15px;
}

.images-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  min-height: 100px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  border: 2px dashed #ddd;
}

.image-item {
  position: relative;
  width: 180px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  touch-action: none; /* 防止移动端触摸事件冲突 */
  display: flex;
  align-items: center;
  padding: 10px;
  background: white;
}

.drag-handle {
  font-size: 16px;
  color: #999;
  cursor: grab;
  margin-right: 10px;
  padding: 2px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.drag-handle:hover {
  color: #666;
  background: rgba(255, 255, 255, 1);
}

.image-upload-item {
  width: 180px;
  height: 100px;
  border-radius: 6px;
  border: 2px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.image-upload-item:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.image-upload-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
}

.image-upload-label:hover {
  color: #667eea;
}

.image-upload-label .upload-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.image-upload-label span {
  font-size: 12px;
  color: #666;
  text-align: center;
}

.image-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.image-item.dragging {
  opacity: 0.7;
  transform: rotate(5deg);
}

.image-item.drag-over {
  border: 2px dashed #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.image-item img {
  width: 60px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 10px;
  flex-shrink: 0;
}

.image-order {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  margin-right: 10px;
  min-width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.remove-image {
  background: rgba(231, 76, 60, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
  margin-left: auto;
}

.remove-image:hover {
  background: rgba(192, 57, 43, 1);
  transform: scale(1.1);
}

.add-chapter-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s ease;
  margin-top: 10px;
}

.add-chapter-btn:hover {
  background: #5a6fea;
}

.form-actions {
  display: flex;
  gap: 20px;
  margin-top: 40px;
  justify-content: flex-end;
}

.submit-btn {
  background: #27ae60;
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: background 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  background: #229954;
}

.submit-btn:disabled {
  background: #95a5a6;
  cursor: not-allowed;
}

.cancel-btn {
  background: #f5f5f5;
  color: #34495e;
  border: 1px solid #ddd;
  padding: 12px 30px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

@media (max-width: 768px) {
  .form-container {
    padding: 15px;
  }
  
  .form-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 20px;
  }
  
  .form-header h1 {
    font-size: 18px;
  }

  .form-content {
    padding: 15px;
  }

  .form-group {
    margin-bottom: 20px;
  }
  
  .form-group label {
    font-size: 14px;
    margin-bottom: 8px;
  }
  
  .form-group input,
  .form-group textarea,
  .form-group select {
    padding: 10px;
    font-size: 14px;
  }
  
  .form-group textarea {
    min-height: 100px;
  }

  .chapter-info {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .chapter-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .chapter-title-input {
    flex: 1;
  }
  
  .remove-chapter {
    padding: 6px 12px;
    font-size: 12px;
  }

  .form-actions {
    flex-direction: column;
    gap: 10px;
    margin-top: 30px;
  }

  .submit-btn,
  .cancel-btn {
    width: 100%;
    padding: 12px;
    font-size: 14px;
  }
  
  .cover-upload {
    height: 150px;
  }
  
  .cover-upload-label {
    font-size: 14px;
  }
  
  .cover-upload-label span {
    font-size: 32px;
  }
  
  .cover-upload-label .cover-image {
    width: 120px;
    height: 160px;
  }
  
  /* 移动端图片预览垂直列表 */
  .images-preview {
    flex-direction: column;
    flex-wrap: nowrap;
    gap: 12px;
    padding: 12px;
  }

  .image-item {
    width: 100%;
    height: auto;
    min-height: 100px;
    display: flex;
    align-items: center;
    padding: 10px;
    background: #f9f9f9;
    border-radius: 8px;
    position: relative;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
    touch-action: none;
  }

  .image-item img {
    width: 80px;
    height: 80px;
    object-fit: cover;
    border-radius: 6px;
    flex-shrink: 0;
    pointer-events: none;
  }

  .image-order {
    position: static;
    background: #667eea;
    color: white;
    font-size: 12px;
    padding: 4px 10px;
    border-radius: 4px;
    margin-left: 10px;
    flex-shrink: 0;
  }

  .remove-image {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(231, 76, 60, 0.9);
    color: white;
    border: none;
    cursor: pointer;
    z-index: 10;
  }

  .remove-image:hover {
    background: rgba(192, 57, 43, 0.9);
  }

  .image-upload-item {
    width: 100%;
    height: 80px;
    border-radius: 8px;
  }

  .add-chapter-btn {
    padding: 10px 20px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .form-header h1 {
    font-size: 16px;
  }

  .cover-upload-label .cover-image {
    width: 100px;
    height: 140px;
  }

  .form-content {
    padding: 10px;
  }

  .form-group input,
  .form-group textarea,
  .form-group select {
    padding: 8px;
    font-size: 13px;
  }

  .submit-btn,
  .cancel-btn {
    padding: 10px;
    font-size: 13px;
  }

  .image-item {
    min-height: 90px;
  }

  .image-item img {
    width: 70px;
    height: 70px;
  }
}
</style>