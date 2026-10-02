<template>
  <div class="cloud-workspace">
    <header class="cloud-header">
      <div class="header-left">
        <HardDriveIcon class="header-icon" />
        <h1>{{ t('cloudDrive.title') }}</h1>
        <span v-if="currentFolderId" class="breadcrumb">
          <ChevronRightIcon class="chevron" />
          <span>{{ currentFolderName || t('cloudDrive.currentFolder') }}</span>
        </span>
      </div>
      <div class="header-actions">
        <button v-if="currentFolderId" @click="goBack" class="action-btn back">
          <ArrowLeftIcon />
          <span>{{ t('common.back') }}</span>
        </button>
        <button @click="showNewFolderModal = true" class="action-btn create">
          <FolderPlusIcon />
          <span>{{ t('cloudDrive.newFolder') }}</span>
        </button>
        <button @click="triggerFileUpload" class="action-btn upload">
          <UploadIcon />
          <span>{{ t('cloudDrive.upload') }}</span>
        </button>
        <input
          type="file"
          ref="fileInput"
          @change="handleFileUpload"
          multiple
          style="display: none"
        />
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>{{ t('common.loading') }}</p>
    </div>

    <div v-else-if="files.length === 0" class="empty-state">
      <CloudOffIcon :size="64" />
      <h3>{{ t('cloudDrive.empty') }}</h3>
      <p>{{ t('cloudDrive.emptyHint') }}</p>
      <button @click="triggerFileUpload" class="upload-btn">
        <UploadIcon />
        {{ t('cloudDrive.upload') }}
      </button>
    </div>

    <div v-else class="file-container">
      <div class="view-controls">
        <span class="file-count">{{ files.length }} {{ t('cloudDrive.items') }}</span>
      </div>

      <div class="file-grid">
        <div
          v-for="file in files"
          :key="file.id"
          :class="['file-card', { folder: file.file_type === 'folder' }]"
          @click="handleFileClick(file)"
        >
          <div class="file-icon">
            <FolderIcon v-if="file.file_type === 'folder'" />
            <FileTextIcon v-else-if="isDocument(file.name)" />
            <FileImageIcon v-else-if="isImage(file.name)" />
            <FileVideoIcon v-else-if="isVideo(file.name)" />
            <FileIcon v-else />
          </div>
          <div class="file-info">
            <span class="file-name" :title="file.name">{{ file.name }}</span>
            <span class="file-meta">
              <span v-if="file.file_type === 'file'" class="file-size">{{ file.formatted_size }}</span>
              <span class="file-date">{{ formatDate(file.created_at) }}</span>
            </span>
          </div>
          <div class="file-actions" @click.stop>
            <button
              v-if="file.file_type === 'file'"
              @click="downloadFile(file)"
              class="icon-btn download"
              :title="t('common.download')"
            >
              <DownloadIcon />
            </button>
            <button
              @click="deleteFile(file)"
              class="icon-btn delete"
              :title="t('common.delete')"
            >
              <TrashIcon />
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="uploading" class="upload-progress">
      <div class="progress-bar">
        <div class="progress-fill"></div>
      </div>
      <span>{{ t('cloudDrive.uploading') }}</span>
    </div>

    <div v-if="showNewFolderModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>{{ t('cloudDrive.createFolder') }}</h3>
          <button @click="closeModal" class="close-btn">
            <XIcon />
          </button>
        </div>
        <div class="modal-body">
          <div class="input-group">
            <FolderIcon class="input-icon" />
            <input
              v-model="newFolderName"
              type="text"
              :placeholder="t('cloudDrive.folderNamePlaceholder')"
              @keyup.enter="createFolder"
              ref="folderNameInput"
            />
          </div>
          <div class="modal-actions">
            <button @click="closeModal" class="btn cancel">{{ t('common.cancel') }}</button>
            <button @click="createFolder" class="btn create">{{ t('common.create') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import apiClient from '../../api/client'
import { useI18n } from 'vue-i18n'


const { t } = useI18n()

const ts = (key) => {
  const translations = {
    'cloud.title': '我的云盘',
    'cloud.currentFolder': '当前文件夹',
    'cloud.newFolder': '新建文件夹',
    'cloud.upload': '上传文件',
    'cloud.empty': '云盘为空',
    'cloud.emptyHint': '上传一些文件开始使用',
    'cloud.items': '个项目',
    'cloud.uploading': '上传中...',
    'cloud.createFolder': '新建文件夹',
    'cloud.folderNamePlaceholder': '输入文件夹名称',
    'common.back': '返回上级',
    'common.loading': '加载中...',
    'common.download': '下载',
    'common.delete': '删除',
    'common.cancel': '取消',
    'common.create': '创建',
  }
  return translations[key] || key
}

const files = ref([])
const loading = ref(false)
const uploading = ref(false)
const currentFolderId = ref(null)
const currentFolderName = ref('')
const showNewFolderModal = ref(false)
const newFolderName = ref('')
const fileInput = ref(null)
const folderNameInput = ref(null)

const fetchFiles = async () => {
  console.log('开始获取文件列表...')
  loading.value = true
  try {
    const params = currentFolderId.value ? { parent: currentFolderId.value } : {}
    console.log('请求参数:', params)
    const response = await apiClient.get('/cloud/', { params })
    console.log('API 响应:', response.data)
    const data = response.data.results || response.data
    files.value = Array.isArray(data) ? data : []
    console.log('文件列表:', files.value)
  } catch (error) {
    console.error('获取文件列表失败:', error)
    files.value = []
  } finally {
    loading.value = false
    console.log('加载状态设置为:', loading.value)
  }
}

const handleFileClick = (file) => {
  if (file.file_type === 'folder') {
    currentFolderId.value = file.id
    currentFolderName.value = file.name
    fetchFiles()
  } else {
    window.open(file.file_url, '_blank')
  }
}

const goBack = async () => {
  if (currentFolderId.value) {
    try {
      const response = await apiClient.get(`/cloud/${currentFolderId.value}/`)
      if (response.data.parent) {
        currentFolderId.value = response.data.parent
        const parentResponse = await apiClient.get(`/cloud/${response.data.parent}/`)
        currentFolderName.value = parentResponse.data.name
      } else {
        currentFolderId.value = null
        currentFolderName.value = ''
      }
    } catch {
      currentFolderId.value = null
      currentFolderName.value = ''
    }
    fetchFiles()
  }
}

const createFolder = async () => {
  if (!newFolderName.value.trim()) return
  try {
    await apiClient.post('/cloud/create-folder/', {
      name: newFolderName.value,
      parent: currentFolderId.value
    })
    closeModal()
    fetchFiles()
  } catch {
    console.error('创建文件夹失败')
  }
}

const closeModal = () => {
  showNewFolderModal.value = false
  newFolderName.value = ''
}

const triggerFileUpload = () => {
  fileInput.value.click()
}

const handleFileUpload = async (event) => {
  const uploadFiles = event.target.files
  if (!uploadFiles.length) return

  uploading.value = true

  for (const file of uploadFiles) {
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

  uploading.value = false
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
  } catch {
    console.error('删除失败')
  }
}

const isDocument = (name) => {
  const ext = name.split('.').pop().toLowerCase()
  return ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'].includes(ext)
}

const isImage = (name) => {
  const ext = name.split('.').pop().toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)
}

const isVideo = (name) => {
  const ext = name.split('.').pop().toLowerCase()
  return ['mp4', 'avi', 'mov', 'mkv', 'webm'].includes(ext)
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(() => {
  fetchFiles()
})
</script>

<style scoped>
.cloud-workspace {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--titlebar-height));
  background: var(--bg-primary);
}

.cloud-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-subtle);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 24px;
  height: 24px;
  color: var(--accent-primary);
}

.cloud-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 14px;
}

.breadcrumb .chevron {
  width: 16px;
  height: 16px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.action-btn.back {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.action-btn.back:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}

.action-btn.create {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.action-btn.create:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}

.action-btn.upload {
  background: var(--accent-primary);
  color: white;
}

.action-btn.upload:hover {
  background: var(--accent-hover);
}

.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-tertiary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-default);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-tertiary);
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-secondary);
  margin: 0;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 10px 20px;
  background: var(--accent-primary);
  color: white;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  transition: background var(--transition-fast);
}

.upload-btn:hover {
  background: var(--accent-hover);
}

.upload-btn svg {
  width: 16px;
  height: 16px;
}

.file-container {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}

.view-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.file-count {
  font-size: 13px;
  color: var(--text-tertiary);
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.file-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.file-card:hover {
  border-color: var(--border-default);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.file-card.folder {
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-tertiary) 100%);
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.file-card.folder .file-icon {
  background: linear-gradient(135deg, var(--accent-primary) 0%, var(--accent-hover) 100%);
}

.file-icon svg {
  width: 28px;
  height: 28px;
  color: var(--text-secondary);
}

.file-card.folder .file-icon svg {
  color: white;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--text-tertiary);
}

.file-size {
  color: var(--accent-primary);
}

.file-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.file-card:hover .file-actions {
  opacity: 1;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.icon-btn svg {
  width: 14px;
  height: 14px;
  color: var(--text-secondary);
}

.icon-btn.download:hover {
  background: var(--accent-primary);
}

.icon-btn.download:hover svg {
  color: white;
}

.icon-btn.delete:hover {
  background: var(--accent-danger);
}

.icon-btn.delete:hover svg {
  color: white;
}

.upload-progress {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.progress-bar {
  width: 200px;
  height: 4px;
  background: var(--bg-tertiary);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  width: 30%;
  height: 100%;
  background: var(--accent-primary);
  border-radius: 2px;
  animation: progress 1.5s ease-in-out infinite;
}

@keyframes progress {
  0% { width: 0%; }
  50% { width: 70%; }
  100% { width: 100%; }
}

.upload-progress span {
  font-size: 12px;
  color: var(--text-secondary);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 90%;
  max-width: 420px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.close-btn:hover {
  background: var(--bg-elevated);
}

.close-btn svg {
  width: 16px;
  height: 16px;
  color: var(--text-secondary);
}

.modal-body {
  padding: 20px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  margin-bottom: 16px;
}

.input-group:focus-within {
  border-color: var(--accent-primary);
}

.input-icon {
  width: 18px;
  height: 18px;
  color: var(--text-tertiary);
}

.input-group input {
  flex: 1;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 14px;
}

.input-group input::placeholder {
  color: var(--text-tertiary);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn.cancel {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.btn.cancel:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}

.btn.create {
  background: var(--accent-primary);
  color: white;
}

.btn.create:hover {
  background: var(--accent-hover);
}
</style>

<script>
const HardDriveIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="12" x2="2" y2="12"/><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/><line x1="6" y1="16" x2="6.01" y2="16"/><line x1="10" y1="16" x2="10.01" y2="16"/></svg>`
}
const ChevronRightIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>`
}
const ArrowLeftIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>`
}
const FolderPlusIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/><line x1="12" y1="11" x2="12" y2="17"/><line x1="9" y1="14" x2="15" y2="14"/></svg>`
}
const UploadIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>`
}
const CloudOffIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="2" y1="2" x2="22" y2="22"/><path d="M9.34 9.34a4 4 0 1 0-5.66-5.66"/><path d="M7.72 18.53a7 7 0 0 0 9.15-9.15"/><path d="M21 17c0 2.76-2.24 5-5 5H8c-2.76 0-5-2.24-5-5"/><path d="M21 8c0 2.76-2.24 5-5 5H8c-2.76 0-5-2.24-5-5"/></svg>`
}
const FolderIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>`
}
const FileIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>`
}
const FileTextIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`
}
const FileImageIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>`
}
const FileVideoIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"/><line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/><line x1="2" y1="12" x2="22" y2="12"/><line x1="2" y1="7" x2="7" y2="7"/><line x1="2" y1="17" x2="7" y2="17"/><line x1="17" y1="17" x2="22" y2="17"/><line x1="17" y1="7" x2="22" y2="7"/></svg>`
}
const DownloadIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>`
}
const TrashIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>`
}
const XIcon = {
  template: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
}
</script>