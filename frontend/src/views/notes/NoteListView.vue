<template>
  <div class="notes-page">
    <div class="notes-header">
      <h1>我的笔记</h1>
      <button @click="createNote" class="create-btn">
        <span>➕</span>
        <span>新建笔记</span>
      </button>
    </div>

    <div class="notes-content">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="error" class="error">
        <p>{{ error }}</p>
        <button @click="fetchNotes" class="retry-btn">重试</button>
      </div>

      <div v-else-if="notes.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <p>暂无笔记</p>
        <p class="hint">点击"新建笔记"开始记录</p>
      </div>

      <div v-else class="notes-grid">
        <div
          v-for="note in notes"
          :key="note.id"
          class="note-card"
          :class="{ active: selectedNote?.id === note.id }"
          @click="selectNote(note)"
        >
          <div class="note-header">
            <h3 class="note-title">{{ note.title || '无标题' }}</h3>
            <span class="note-date">{{ formatDate(note.updated_at) }}</span>
          </div>
          <p class="note-preview">{{ getPreview(note.content) }}</p>
        </div>
      </div>

      <div v-if="selectedNote || isEditing" class="note-editor">
        <div class="editor-header">
          <input
            v-model="editForm.title"
            type="text"
            placeholder="笔记标题..."
            class="title-input"
          />
          <div class="editor-actions">
            <label class="import-btn">
              <span>📥</span>
              <span>导入MD</span>
              <input
                type="file"
                accept=".md,.markdown,.txt"
                @change="importMdFile"
                style="display: none"
              />
            </label>
            <label class="image-btn" :class="{ disabled: isPreview }">
              <span>🖼️</span>
              <span>上传图片</span>
              <input
                type="file"
                accept="image/*"
                :disabled="isPreview"
                @change="uploadImage"
                style="display: none"
              />
            </label>
            <button
              @click="togglePreview"
              class="preview-btn"
              :class="{ active: isPreview }"
            >
              <span>👁️</span>
              <span>{{ isPreview ? '编辑' : '预览' }}</span>
            </button>
            <button @click="saveNote" class="save-btn" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
            <button @click="cancelEdit" class="cancel-btn">取消</button>
            <button
              v-if="selectedNote"
              @click="deleteNote"
              class="delete-btn"
            >
              删除
            </button>
          </div>
        </div>

        <div v-if="isPreview" class="markdown-preview" v-html="renderedContent"></div>
        <div v-else class="editor-body">
          <div class="editor-toolbar">
            <button @click="insertFormat('**', '**')" title="粗体">B</button>
            <button @click="insertFormat('*', '*')" title="斜体">I</button>
            <button @click="insertFormat('~~', '~~')" title="删除线">S</button>
            <span class="toolbar-divider">|</span>
            <button @click="insertFormat('# ', '')" title="一级标题">H1</button>
            <button @click="insertFormat('## ', '')" title="二级标题">H2</button>
            <button @click="insertFormat('### ', '')" title="三级标题">H3</button>
            <span class="toolbar-divider">|</span>
            <button @click="insertFormat('- ', '')" title="无序列表">•</button>
            <button @click="insertFormat('1. ', '')" title="有序列表">1.</button>
            <button @click="insertFormat('> ', '')" title="引用">❝</button>
            <span class="toolbar-divider">|</span>
            <button @click="insertFormat('`', '`')" title="行内代码">code</button>
            <button @click="insertFormat('\n```\n', '\n```\n')" title="代码块">&lt;/&gt;</button>
            <span class="toolbar-divider">|</span>
            <button @click="insertFormat('[', '](url)')" title="链接">🔗</button>
          </div>
          <textarea
            ref="textareaRef"
            v-model="editForm.content"
            placeholder="开始写笔记... 支持Markdown语法&#10;&#10;例如：&#10;# 标题&#10;**粗体**&#10;*斜体*&#10;- 列表项"
            class="content-input"
          ></textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import apiClient from '../../api/client'

marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
})

const notes = ref([])
const loading = ref(true)
const error = ref('')
const selectedNote = ref(null)
const isEditing = ref(false)
const isPreview = ref(false)
const saving = ref(false)
const uploadingImage = ref(false)
const textareaRef = ref(null)

const editForm = ref({
  title: '',
  content: ''
})

const renderedContent = computed(() => {
  if (!editForm.value.content) return ''
  return marked(editForm.value.content)
})

const fetchNotes = async () => {
  try {
    loading.value = true
    error.value = ''
    const response = await apiClient.get('/notes/')
    notes.value = response.data
  } catch (e) {
    error.value = e.response?.data?.detail || '获取笔记失败'
  } finally {
    loading.value = false
  }
}

const createNote = () => {
  selectedNote.value = null
  isEditing.value = true
  isPreview.value = false
  editForm.value = {
    title: '',
    content: ''
  }
}

const selectNote = (note) => {
  selectedNote.value = note
  isEditing.value = true
  isPreview.value = false
  editForm.value = {
    title: note.title,
    content: note.content
  }
}

const togglePreview = () => {
  isPreview.value = !isPreview.value
}

const insertFormat = (before, after) => {
  const textarea = textareaRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = editForm.value.content.substring(start, end)

  const newText =
    editForm.value.content.substring(0, start) +
    before +
    selectedText +
    after +
    editForm.value.content.substring(end)

  editForm.value.content = newText

  setTimeout(() => {
    textarea.focus()
    const newCursorPos = selectedText ? start + before.length + selectedText.length + after.length : start + before.length
    textarea.setSelectionRange(newCursorPos, newCursorPos)
  }, 0)
}

const uploadImage = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  try {
    uploadingImage.value = true
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post('/upload/image/', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    let path = response.data.path
    if (!path.startsWith('media/')) {
      path = 'media/' + path
    }

    const imageUrl = `/media/${path}`
    const imageMarkdown = `![${file.name}](${imageUrl})`

    editForm.value.content += '\n' + imageMarkdown

  } catch (error) {
    console.error('上传图片失败:', error)
    alert('上传图片失败，请重试')
  } finally {
    uploadingImage.value = false
    event.target.value = ''
  }
}

const importMdFile = (event) => {
  const file = event.target.files[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    const content = e.target.result
    if (!editForm.value.title) {
      editForm.value.title = file.name.replace(/\.(md|markdown|txt)$/i, '')
    }
    editForm.value.content = content
  }
  reader.readAsText(file)
  event.target.value = ''
}

const saveNote = async () => {
  if (!editForm.value.title.trim() && !editForm.value.content.trim()) {
    return
  }

  try {
    saving.value = true
    const data = {
      title: editForm.value.title || '无标题',
      content: editForm.value.content
    }

    if (selectedNote.value) {
      await apiClient.put(`/notes/${selectedNote.value.id}/`, data)
    } else {
      await apiClient.post('/notes/', data)
    }

    await fetchNotes()
    cancelEdit()
  } catch (e) {
    console.error('保存笔记失败:', e)
  } finally {
    saving.value = false
  }
}

const cancelEdit = () => {
  selectedNote.value = null
  isEditing.value = false
  isPreview.value = false
  editForm.value = {
    title: '',
    content: ''
  }
}

const deleteNote = async () => {
  if (!selectedNote.value) return

  if (!confirm('确定要删除这条笔记吗？')) {
    return
  }

  try {
    await apiClient.delete(`/notes/${selectedNote.value.id}/`)
    await fetchNotes()
    cancelEdit()
  } catch (e) {
    console.error('删除笔记失败:', e)
  }
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)} 分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)} 小时前`
  } else if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)} 天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  }
}

const getPreview = (content) => {
  if (!content) return '暂无内容'
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

onMounted(fetchNotes)
</script>

<style scoped>
.notes-page {
  padding: 20px 0;
  min-height: calc(100vh - 100px);
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.notes-header h1 {
  font-size: 28px;
  color: #2c3e50;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
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

.error {
  text-align: center;
  padding: 40px;
  color: #e74c3c;
}

.retry-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #f9f9f9;
  border-radius: 12px;
  border: 2px dashed #ddd;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state p {
  font-size: 18px;
  color: #666;
  margin: 0 0 10px 0;
}

.empty-state .hint {
  font-size: 14px;
  color: #999;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.note-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.note-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
}

.note-card.active {
  border-color: #667eea;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.note-title {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
  flex: 1;
  word-break: break-word;
}

.note-date {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 10px;
}

.note-preview {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.note-editor {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f9f9f9;
  border-bottom: 1px solid #eee;
  gap: 15px;
  flex-wrap: wrap;
}

.title-input {
  flex: 1;
  min-width: 200px;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  outline: none;
  transition: border-color 0.3s ease;
}

.title-input:focus {
  border-color: #667eea;
}

.editor-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.import-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #a5d6a7;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.import-btn:hover {
  background: #c8e6c9;
}

.image-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #e3f2fd;
  color: #1565c0;
  border: 1px solid #90caf9;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.image-btn:hover:not(.disabled) {
  background: #bbdefb;
}

.image-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.preview-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #fff3e0;
  color: #e65100;
  border: 1px solid #ffcc80;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.preview-btn:hover,
.preview-btn.active {
  background: #ffe0b2;
}

.save-btn {
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.save-btn:hover:not(:disabled) {
  background: #5a6fea;
}

.save-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 10px 20px;
  background: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.delete-btn {
  padding: 10px 20px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background: #c0392b;
}

.content-input {
  width: 100%;
  min-height: 400px;
  padding: 20px;
  border: none;
  font-size: 16px;
  line-height: 1.8;
  resize: vertical;
  outline: none;
  font-family: 'Consolas', 'Monaco', monospace;
}

.editor-body {
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 15px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.editor-toolbar button {
  padding: 6px 12px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  font-weight: bold;
  color: #333;
  transition: all 0.2s ease;
}

.editor-toolbar button:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.toolbar-divider {
  color: #ddd;
  margin: 0 5px;
}

.markdown-preview {
  padding: 20px;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  font-size: 16px;
  line-height: 1.8;
}

.markdown-preview :deep(h1) {
  font-size: 28px;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
  margin: 20px 0;
  color: #2c3e50;
}

.markdown-preview :deep(h2) {
  font-size: 24px;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
  margin: 18px 0;
  color: #34495e;
}

.markdown-preview :deep(h3) {
  font-size: 20px;
  margin: 16px 0;
  color: #444;
}

.markdown-preview :deep(h4),
.markdown-preview :deep(h5),
.markdown-preview :deep(h6) {
  font-size: 16px;
  margin: 14px 0;
  color: #555;
}

.markdown-preview :deep(p) {
  margin: 12px 0;
}

.markdown-preview :deep(code) {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  color: #e74c3c;
}

.markdown-preview :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 15px 0;
}

.markdown-preview :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.markdown-preview :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 15px;
  margin: 15px 0;
  color: #666;
  background: #f9f9f9;
  padding: 10px 15px;
  border-radius: 0 8px 8px 0;
}

.markdown-preview :deep(ul),
.markdown-preview :deep(ol) {
  margin: 12px 0;
  padding-left: 25px;
}

.markdown-preview :deep(li) {
  margin: 5px 0;
}

.markdown-preview :deep(a) {
  color: #667eea;
  text-decoration: none;
}

.markdown-preview :deep(a:hover) {
  text-decoration: underline;
}

.markdown-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 15px 0;
}

.markdown-preview :deep(th),
.markdown-preview :deep(td) {
  border: 1px solid #ddd;
  padding: 10px;
  text-align: left;
}

.markdown-preview :deep(th) {
  background: #f5f5f5;
  font-weight: bold;
}

.markdown-preview :deep(hr) {
  border: none;
  border-top: 1px solid #ddd;
  margin: 20px 0;
}

.markdown-preview :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .notes-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .create-btn {
    width: 100%;
    justify-content: center;
  }

  .notes-grid {
    grid-template-columns: 1fr;
  }

  .editor-header {
    flex-direction: column;
    align-items: stretch;
  }

  .title-input {
    width: 100%;
  }

  .editor-actions {
    justify-content: flex-start;
  }

  .content-input {
    min-height: 300px;
  }

  .markdown-preview {
    min-height: 300px;
    max-height: 500px;
  }
}
</style>
