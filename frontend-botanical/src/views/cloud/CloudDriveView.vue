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

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
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
  padding: 20px 0;
}

.cloud-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--flora-base-dark);
}

.cloud-header h2 {
  margin: 0;
  font-family: var(--font-heading);
  color: var(--flora-stem);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.back-btn,
.create-folder-btn,
.upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn {
  background: var(--flora-base);
  color: var(--flora-stem);
}

.back-btn:hover {
  background: var(--flora-base-dark);
}

.create-folder-btn {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
}

.upload-btn {
  background: var(--flora-stem);
  color: white;
}

.create-folder-btn:hover,
.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--flora-shadow);
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
  border: 3px solid var(--flora-base-dark);
  border-top-color: var(--flora-leaf);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 15px;
}

.empty-state p {
  font-family: var(--font-body);
  color: var(--flora-stem-light);
  font-size: 1rem;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  background: var(--flora-base-light);
  border-radius: var(--radius-md);
  box-shadow: 0 2px 8px var(--flora-shadow);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.file-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 16px var(--flora-shadow);
}

.file-item.folder {
  background: var(--flora-base);
}

.file-icon {
  font-size: 32px;
  margin-right: 16px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--flora-stem);
  margin-bottom: 4px;
}

.file-meta {
  font-family: var(--font-body);
  font-size: 0.75rem;
  color: var(--flora-stem-light);
  display: flex;
  gap: 15px;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 1rem;
}

.action-btn.download {
  background: var(--flora-base);
}

.action-btn.download:hover {
  background: var(--flora-leaf);
}

.action-btn.delete {
  background: var(--flora-base);
}

.action-btn.delete:hover {
  background: var(--flora-bloom);
}

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
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 30px var(--flora-shadow);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--flora-base-dark);
}

.modal-header h3 {
  margin: 0;
  font-family: var(--font-heading);
  color: var(--flora-stem);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.25rem;
  cursor: pointer;
  color: var(--flora-stem-light);
  transition: color var(--transition-fast);
}

.close-btn:hover {
  color: var(--flora-stem);
}

.modal-body {
  padding: 20px;
}

.modal-body input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  margin-bottom: 16px;
  background: var(--flora-base);
  color: var(--flora-stem);
}

.modal-body input:focus {
  outline: none;
  border-color: var(--flora-leaf);
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--flora-shadow);
}

@media (max-width: 768px) {
  .cloud-header h2 {
    display: none;
  }

  .cloud-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .back-btn,
  .create-folder-btn,
  .upload-btn {
    padding: 8px 14px;
    font-size: 0.8rem;
  }

  .file-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>