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
  font-family: var(--font-heading);
  font-size: 1.75rem;
  color: var(--flora-stem);
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.create-btn:hover {
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

.error {
  text-align: center;
  padding: 40px;
  color: var(--flora-bloom-dark);
}

.retry-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: var(--flora-leaf);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  border: 2px dashed var(--flora-base-dark);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state p {
  font-family: var(--font-body);
  font-size: 1.125rem;
  color: var(--flora-stem);
  margin: 0 0 10px 0;
}

.empty-state .hint {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.note-card {
  background: var(--flora-base-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 2px 10px var(--flora-shadow);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.note-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px var(--flora-shadow);
}

.note-card.active {
  border-color: var(--flora-leaf);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.note-title {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0;
  flex: 1;
  word-break: break-word;
}

.note-date {
  font-family: var(--font-body);
  font-size: 0.75rem;
  color: var(--flora-stem-light);
  flex-shrink: 0;
  margin-left: 10px;
}

.note-preview {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem);
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.note-editor {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 20px var(--flora-shadow);
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: var(--flora-base);
  border-bottom: 1px solid var(--flora-base-dark);
  gap: 15px;
  flex-wrap: wrap;
}

.title-input {
  flex: 1;
  min-width: 200px;
  padding: 10px 15px;
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  outline: none;
  transition: border-color var(--transition-fast);
  background: var(--flora-base-light);
  color: var(--flora-stem);
}

.title-input:focus {
  border-color: var(--flora-leaf);
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
  background: var(--flora-base-light);
  color: var(--flora-stem);
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.import-btn:hover {
  background: var(--flora-base);
}

.image-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--flora-base-light);
  color: var(--flora-stem);
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.image-btn:hover:not(.disabled) {
  background: var(--flora-base);
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
  background: var(--flora-base-light);
  color: var(--flora-stem);
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.preview-btn:hover,
.preview-btn.active {
  background: var(--flora-base);
}

.save-btn {
  padding: 10px 20px;
  background: var(--flora-leaf);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.save-btn:hover:not(:disabled) {
  background: var(--flora-leaf-dark);
}

.save-btn:disabled {
  background: var(--flora-base-dark);
  cursor: not-allowed;
}

.cancel-btn {
  padding: 10px 20px;
  background: var(--flora-base);
  color: var(--flora-stem);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.cancel-btn:hover {
  background: var(--flora-base-dark);
}

.delete-btn {
  padding: 10px 20px;
  background: var(--flora-bloom);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.delete-btn:hover {
  background: var(--flora-bloom-dark);
}

.content-input {
  width: 100%;
  min-height: 400px;
  padding: 20px;
  border: none;
  font-family: var(--font-body);
  font-size: 1rem;
  line-height: 1.8;
  resize: vertical;
  outline: none;
  font-family: 'Consolas', 'Monaco', monospace;
  background: var(--flora-base-light);
  color: var(--flora-stem);
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
  background: var(--flora-base);
  border-bottom: 1px solid var(--flora-base-dark);
  flex-wrap: wrap;
}

.editor-toolbar button {
  padding: 6px 12px;
  background: var(--flora-base-light);
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.8rem;
  font-weight: bold;
  color: var(--flora-stem);
  transition: all var(--transition-fast);
}

.editor-toolbar button:hover {
  background: var(--flora-leaf);
  color: white;
  border-color: var(--flora-leaf);
}

.toolbar-divider {
  color: var(--flora-base-dark);
  margin: 0 5px;
}

.markdown-preview {
  padding: 20px;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  font-family: var(--font-body);
  font-size: 1rem;
  line-height: 1.8;
  background: var(--flora-base-light);
  color: var(--flora-stem);
}

.markdown-preview :deep(h1) {
  font-family: var(--font-heading);
  font-size: 1.75rem;
  border-bottom: 2px solid var(--flora-base-dark);
  padding-bottom: 10px;
  margin: 20px 0;
  color: var(--flora-stem);
}

.markdown-preview :deep(h2) {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  border-bottom: 1px solid var(--flora-base-dark);
  padding-bottom: 8px;
  margin: 18px 0;
  color: var(--flora-stem);
}

.markdown-preview :deep(h3) {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  margin: 16px 0;
  color: var(--flora-stem);
}

.markdown-preview :deep(h4),
.markdown-preview :deep(h5),
.markdown-preview :deep(h6) {
  font-family: var(--font-heading);
  font-size: 1rem;
  margin: 14px 0;
  color: var(--flora-stem);
}

.markdown-preview :deep(p) {
  margin: 12px 0;
}

.markdown-preview :deep(code) {
  background: var(--flora-base);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.875rem;
  color: var(--flora-bloom);
}

.markdown-preview :deep(pre) {
  background: var(--flora-stem);
  color: var(--flora-base-light);
  padding: 15px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 15px 0;
}

.markdown-preview :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.markdown-preview :deep(blockquote) {
  border-left: 4px solid var(--flora-leaf);
  padding-left: 15px;
  margin: 15px 0;
  color: var(--flora-stem);
  background: var(--flora-base);
  padding: 10px 15px;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
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
  color: var(--flora-leaf);
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
  border: 1px solid var(--flora-base-dark);
  padding: 10px;
  text-align: left;
}

.markdown-preview :deep(th) {
  background: var(--flora-base);
  font-weight: bold;
}

.markdown-preview :deep(hr) {
  border: none;
  border-top: 1px solid var(--flora-base-dark);
  margin: 20px 0;
}

.markdown-preview :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-md);
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

@media (max-width: 768px) {
  .notes-header h1 {
    display: none;
  }

  .notes-header {
    justify-content: center;
  }
}
</style>