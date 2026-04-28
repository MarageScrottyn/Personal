<template>
  <div class="cloud-drive">
    <div class="cloud-header">
      <h2>我的云盘</h2>
      <div class="header-actions">
        <button v-if="currentFolderId" @click="goBack" class="back-btn">
          <span>⬆️</span> 返回上级
        </button>
        <button @click="showNewFolderModal = true" class="create-folder-btn">
          <span>📁</span> 新建文件夹
        </button>
        <button @click="triggerFileUpload" class="upload-btn">
          <span>⬆️</span> 上传文件
        </button>
        <input
          type="file"
          ref="fileInput"
          @change="handleFileUpload"
          multiple
          style="display: none"
        />
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="files.length === 0" class="empty-state">
      <div class="empty-icon">☁️</div>
      <p>云盘为空，上传一些文件吧</p>
    </div>
    <div v-else class="file-list">
      <div
        v-for="file in files"
        :key="file.id"
        :class="['file-item', { folder: file.file_type === 'folder' }]"
        @click="handleFileClick(file)"
      >
        <div class="file-icon">
          {{ file.file_type === 'folder' ? '📁' : getFileIcon(file.name) }}
        </div>
        <div class="file-info">
          <div class="file-name">{{ file.name }}</div>
          <div class="file-meta">
            <span v-if="file.file_type === 'file'">{{ file.formatted_size }}</span>
            <span>{{ formatDate(file.created_at) }}</span>
          </div>
        </div>
        <div class="file-actions">
          <button
            v-if="file.file_type === 'file'"
            @click.stop="downloadFile(file)"
            class="action-btn download"
            title="下载"
          >
            ⬇️
          </button>
          <button
            @click.stop="deleteFile(file)"
            class="action-btn delete"
            title="删除"
          >
            🗑️
          </button>
        </div>
      </div>
    </div>

    <!-- 新建文件夹弹窗 -->
    <div v-if="showNewFolderModal" class="modal" @click="showNewFolderModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新建文件夹</h3>
          <button @click="showNewFolderModal = false" class="close-btn">✕</button>
        </div>
        <div class="modal-body">
          <input
            v-model="newFolderName"
            type="text"
            placeholder="文件夹名称"
            @keyup.enter="createFolder"
          />
          <button @click="createFolder" class="submit-btn">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/api/client'

const files = ref([])
const loading = ref(false)
const currentFolderId = ref(null)
const showNewFolderModal = ref(false)
const newFolderName = ref('')
const fileInput = ref(null)

const fetchFiles = async () => {
  loading.value = true
  try {
    const params = currentFolderId.value ? { parent: currentFolderId.value } : {}
    const response = await apiClient.get('/cloud/', { params })
    files.value = response.data
  } catch (error) {
    console.error('获取文件列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleFileClick = (file) => {
  if (file.file_type === 'folder') {
    currentFolderId.value = file.id
    fetchFiles()
  } else {
    window.open(file.file_url, '_blank')
  }
}

const goBack = async () => {
  if (currentFolderId.value) {
    const response = await apiClient.get(`/cloud/${currentFolderId.value}/`)
    if (response.data.parent) {
      currentFolderId.value = response.data.parent
    } else {
      currentFolderId.value = null
    }
    fetchFiles()
  }
}

const createFolder = async () => {
  if (!newFolderName.value.trim()) {
    alert('请输入文件夹名称')
    return
  }
  try {
    await apiClient.post('/cloud/create-folder/', {
      name: newFolderName.value,
      parent: currentFolderId.value
    })
    showNewFolderModal.value = false
    newFolderName.value = ''
    fetchFiles()
  } catch (error) {
    alert('创建文件夹失败')
  }
}

const triggerFileUpload = () => {
  fileInput.value.click()
}

const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files.length) return

  for (const file of files) {
    const formData = new FormData()
    formData.append('name', file.name)
    formData.append('file_type', 'file')
    formData.append('file_path', file)
    if (currentFolderId.value) {
      formData.append('parent', currentFolderId.value)
    }

    try {
      await apiClient.post('/cloud/', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    } catch (error) {
      console.error('上传文件失败:', error)
    }
  }
  fetchFiles()
  event.target.value = ''
}

const downloadFile = (file) => {
  if (file.file_url) {
    const link = document.createElement('a')
    link.href = file.file_url
    link.download = file.name
    link.click()
  }
}

const deleteFile = async (file) => {
  if (!confirm(`确定要删除 "${file.name}" 吗？`)) return
  try {
    await apiClient.delete(`/cloud/${file.id}/`)
    fetchFiles()
  } catch (error) {
    alert('删除失败')
  }
}

const getFileIcon = (name) => {
  const ext = name.split('.').pop().toLowerCase()
  const icons = {
    pdf: '📕',
    doc: '📘',
    docx: '📘',
    xls: '📗',
    xlsx: '📗',
    ppt: '📙',
    pptx: '📙',
    txt: '📄',
    zip: '🗜️',
    rar: '🗜️',
    jpg: '🖼️',
    jpeg: '🖼️',
    png: '🖼️',
    gif: '🖼️',
    mp4: '🎬',
    mp3: '🎵',
    wav: '🎵'
  }
  return icons[ext] || '📄'
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchFiles()
})
</script>

<style scoped>
.cloud-drive {
  max-width: 1000px;
  margin: 0 auto;
}

.cloud-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.cloud-header h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.back-btn,
.create-folder-btn,
.upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn {
  background: #f0f0f0;
  color: #666;
}

.create-folder-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.upload-btn {
  background: #4caf50;
  color: white;
}

.back-btn:hover,
.create-folder-btn:hover,
.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 15px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
}

.file-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.file-item.folder {
  background: #f8f9fa;
}

.file-icon {
  font-size: 32px;
  margin-right: 15px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.file-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 15px;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
}

.action-btn.download {
  background: #e3f2fd;
}

.action-btn.download:hover {
  background: #bbdefb;
}

.action-btn.delete {
  background: #ffebee;
}

.action-btn.delete:hover {
  background: #ffcdd2;
}

/* 弹窗样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 20px;
}

.modal-body input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 15px;
}

.modal-body input:focus {
  outline: none;
  border-color: #667eea;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

@media (max-width: 768px) {
  .cloud-header h2 {
    display: none;
  }
  
  .cloud-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .file-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
