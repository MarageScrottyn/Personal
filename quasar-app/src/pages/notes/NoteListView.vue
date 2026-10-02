<template>
  <div class="notes-workspace">
    <aside class="notes-sidebar">
      <div class="sidebar-header">
        <FileTextIcon class="header-icon" />
        <span class="header-title">{{ t('note.title') }}</span>
        <button @click="createNote" class="add-btn" :title="t('note.new')">
          <PlusIcon />
        </button>
      </div>

      <div class="sidebar-search">
        <SearchIcon class="search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          :placeholder="t('common.search')"
        />
      </div>

      <div v-if="loading && notes.length === 0" class="sidebar-loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="filteredNotes.length === 0" class="sidebar-empty">
        <FileXIcon :size="32" />
        <p>{{ notes.length === 0 ? t('note.empty') : t('note.noResults') }}</p>
      </div>

      <div v-else class="notes-list">
        <div
          v-for="note in filteredNotes"
          :key="note.id"
          :class="['note-item', { active: selectedNote?.id === note.id }]"
          @click="selectNote(note)"
        >
          <div class="note-item-header">
            <span class="note-item-title">{{ note.title || t('note.untitled') }}</span>
            <span class="note-item-date">{{ formatDate(note.updated_at) }}</span>
          </div>
          <p class="note-item-preview">{{ getPreview(note.content) }}</p>
        </div>
      </div>
    </aside>

    <main class="notes-editor">
      <div v-if="!isEditing" class="editor-placeholder">
        <FileTextIcon :size="64" />
        <h3>{{ t('note.selectOrCreate') }}</h3>
        <p>{{ t('note.hint') }}</p>
        <button @click="createNote" class="create-btn">
          <PlusIcon />
          {{ t('note.new') }}
        </button>
      </div>

      <template v-else>
        <header class="editor-header">
          <input
            v-model="editForm.title"
            type="text"
            :placeholder="t('note.titlePlaceholder')"
            class="title-input"
          />
          <div class="editor-toolbar">
            <label class="toolbar-btn" :title="t('note.importMd')">
              <DownloadIcon />
              <span>{{ t('note.import') }}</span>
              <input
                type="file"
                accept=".md,.markdown,.txt"
                @change="importMdFile"
                style="display: none"
              />
            </label>
            <button
              @click="togglePreview"
              :class="['toolbar-btn', { active: isPreview }]"
              :title="isPreview ? t('note.edit') : t('note.preview')"
            >
              <EyeIcon v-if="!isPreview" />
              <EditIcon v-else />
              <span>{{ isPreview ? t('note.edit') : t('note.preview') }}</span>
            </button>
            <div class="toolbar-divider"></div>
            <button @click="saveNote" class="save-btn" :disabled="saving">
              <CheckIcon v-if="!saving" />
              <LoaderIcon v-else class="spin" />
              <span>{{ saving ? t('common.saving') : t('common.save') }}</span>
            </button>
            <button
              v-if="selectedNote"
              @click="deleteNote"
              class="delete-btn"
              :title="t('common.delete')"
            >
              <TrashIcon />
            </button>
          </div>
        </header>

        <div class="editor-body">
          <div v-if="!isPreview" class="editor-toolbar-inline">
            <button @click="insertFormat('**', '**')" class="format-btn" title="Bold">
              <BoldIcon />
            </button>
            <button @click="insertFormat('*', '*')" class="format-btn" title="Italic">
              <ItalicIcon />
            </button>
            <button @click="insertFormat('~~', '~~')" class="format-btn" title="Strikethrough">
              <StrikethroughIcon />
            </button>
            <span class="toolbar-sep"></span>
            <button @click="insertFormat('# ', '')" class="format-btn" title="H1">H1</button>
            <button @click="insertFormat('## ', '')" class="format-btn" title="H2">H2</button>
            <button @click="insertFormat('### ', '')" class="format-btn" title="H3">H3</button>
            <span class="toolbar-sep"></span>
            <button @click="insertFormat('- ', '')" class="format-btn" title="List">
              <ListIcon />
            </button>
            <button @click="insertFormat('1. ', '')" class="format-btn" title="Numbered">
              <ListOrderedIcon />
            </button>
            <button @click="insertFormat('> ', '')" class="format-btn" title="Quote">
              <QuoteIcon />
            </button>
            <span class="toolbar-sep"></span>
            <button @click="insertFormat('`', '`')" class="format-btn" title="Code">
              <CodeIcon />
            </button>
            <button @click="insertFormat('\n```\n', '\n```\n')" class="format-btn" title="Code Block">
              <SquareCodeIcon />
            </button>
            <span class="toolbar-sep"></span>
            <button @click="insertFormat('[', '](url)')" class="format-btn" title="Link">
              <LinkIcon />
            </button>
            <label class="format-btn" title="Image">
              <ImageIcon />
              <input
                type="file"
                accept="image/*"
                @change="uploadImage"
                style="display: none"
              />
            </label>
          </div>

          <div v-if="isPreview" class="markdown-preview" v-html="renderedContent"></div>
          <textarea
            v-else
            ref="textareaRef"
            v-model="editForm.content"
            :placeholder="t('note.contentPlaceholder')"
            class="content-input"
          ></textarea>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import apiClient from '../../api/client'
import { useI18n } from 'vue-i18n'


const { t, locale } = useI18n()

const FileTextIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
      h('line', { x1: '10', y1: '9', x2: '8', y2: '9' })
    ])
  }
}

const FileXIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '10', y1: '12', x2: '14', y2: '16' }),
      h('line', { x1: '14', y1: '12', x2: '10', y2: '16' })
    ])
  }
}

const PlusIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('line', { x1: '12', y1: '5', x2: '12', y2: '19' }),
      h('line', { x1: '5', y1: '12', x2: '19', y2: '12' })
    ])
  }
}

const SearchIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('circle', { cx: '11', cy: '11', r: '8' }),
      h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
    ])
  }
}

const DownloadIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('path', { d: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4' }),
      h('polyline', { points: '7 10 12 15 17 10' }),
      h('line', { x1: '12', y1: '15', x2: '12', y2: '3' })
    ])
  }
}

const EyeIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('path', { d: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' }),
      h('circle', { cx: '12', cy: '12', r: '3' })
    ])
  }
}

const EditIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('path', { d: 'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7' }),
      h('path', { d: 'M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z' })
    ])
  }
}

const CheckIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('polyline', { points: '20 6 9 17 4 12' })
    ])
  }
}

const LoaderIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'animate-spin' }, [
      h('circle', { cx: '12', cy: '12', r: '10', strokeDasharray: '100', strokeDashoffset: '25' })
    ])
  }
}

const ImageIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('rect', { x: '3', y: '3', width: '18', height: '18', rx: '2', ry: '2' }),
      h('circle', { cx: '8.5', cy: '8.5', r: '1.5' }),
      h('polyline', { points: '21 15 16 10 5 21' })
    ])
  }
}

const TrashIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('polyline', { points: '3 6 5 6 21 6' }),
      h('path', { d: 'M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2' })
    ])
  }
}

const XIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('line', { x1: '18', y1: '6', x2: '6', y2: '18' }),
      h('line', { x1: '6', y1: '6', x2: '18', y2: '18' })
    ])
  }
}

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
const selectedNote = ref(null)
const isEditing = ref(false)
const isPreview = ref(false)
const saving = ref(false)
const searchQuery = ref('')
const textareaRef = ref(null)

const editForm = ref({
  title: '',
  content: ''
})

const filteredNotes = computed(() => {
  if (!searchQuery.value) return notes.value
  const query = searchQuery.value.toLowerCase()
  return notes.value.filter(note =>
    note.title?.toLowerCase().includes(query) ||
    note.content?.toLowerCase().includes(query)
  )
})

const renderedContent = computed(() => {
  if (!editForm.value.content) return ''
  return marked(editForm.value.content)
})

const fetchNotes = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/notes/')
    const data = response.data.results || response.data
    notes.value = Array.isArray(data) ? data : []
    console.log('获取笔记:', notes.value.length)
  } catch (e) {
    console.error('获取笔记失败:', e)
    notes.value = []
  } finally {
    loading.value = false
  }
}

const createNote = () => {
  selectedNote.value = null
  isEditing.value = true
  isPreview.value = false
  editForm.value = { title: '', content: '' }
}

const selectNote = (note) => {
  selectedNote.value = note
  isEditing.value = true
  isPreview.value = false
  editForm.value = { title: note.title, content: note.content }
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
    before + selectedText + after +
    editForm.value.content.substring(end)

  editForm.value.content = newText

  setTimeout(() => {
    textarea.focus()
    const newCursorPos = selectedText
      ? start + before.length + selectedText.length + after.length
      : start + before.length
    textarea.setSelectionRange(newCursorPos, newCursorPos)
  }, 0)
}

const uploadImage = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post('/upload/image/', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    let path = response.data.path
    if (!path.startsWith('media/')) path = 'media/' + path

    const imageMarkdown = `![${file.name}](${path})`
    editForm.value.content += '\n' + imageMarkdown
  } catch (error) {
    console.error('上传图片失败:', error)
  }
  event.target.value = ''
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
  if (!editForm.value.title.trim() && !editForm.value.content.trim()) return

  try {
    saving.value = true
    const data = {
      title: editForm.value.title || t('note.untitled'),
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
  editForm.value = { title: '', content: '' }
}

const deleteNote = async () => {
  if (!selectedNote.value) return
  if (!confirm('确定要删除这条笔记吗？')) return

  try {
    await apiClient.delete(`/notes/${selectedNote.value.id}/`)
    await fetchNotes()
    cancelEdit()
  } catch (e) {
    console.error('删除笔记失败:', e)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return t('common.noDate')
  
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return t('common.noDate')
  
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return t('common.justNow')
  if (diff < 3600000) return `${Math.floor(diff / 60000)} ${t('common.minutesAgo')}`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} ${t('common.hoursAgo')}`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} ${t('common.daysAgo')}`
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const getPreview = (content) => {
  if (!content) return t('note.noContent')
  const text = content.replace(/[#*`~_[\]]/g, '').trim()
  return text.length > 60 ? text.substring(0, 60) + '...' : text
}

onMounted(fetchNotes)
</script>

<style scoped>
.notes-workspace {
  display: flex;
  height: calc(100vh - var(--titlebar-height));
  background: var(--bg-primary);
}

.notes-sidebar {
  width: 280px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.header-icon {
  width: 20px;
  height: 20px;
  color: var(--accent-primary);
}

.header-title {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--accent-primary);
  border-radius: var(--radius-sm);
  color: white;
  transition: background var(--transition-fast);
}

.add-btn:hover {
  background: var(--accent-hover);
}

.add-btn svg {
  width: 16px;
  height: 16px;
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px;
  padding: 8px 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
}

.sidebar-search .search-icon {
  width: 16px;
  height: 16px;
  color: var(--text-tertiary);
}

.sidebar-search input {
  flex: 1;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 13px;
}

.sidebar-search input::placeholder {
  color: var(--text-tertiary);
}

.sidebar-loading,
.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-tertiary);
  gap: 12px;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.notes-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.note-item {
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
  margin-bottom: 4px;
}

.note-item:hover {
  background: var(--bg-tertiary);
}

.note-item.active {
  background: var(--bg-elevated);
  border-left: 3px solid var(--accent-primary);
}

.note-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.note-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.note-item-date {
  font-size: 11px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.note-item-preview {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.notes-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.editor-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  gap: 16px;
}

.editor-placeholder h3 {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-secondary);
  margin: 0;
}

.editor-placeholder p {
  font-size: 13px;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--accent-primary);
  color: white;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  margin-top: 8px;
  transition: background var(--transition-fast);
}

.create-btn:hover {
  background: var(--accent-hover);
}

.create-btn svg {
  width: 16px;
  height: 16px;
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-subtle);
}

.title-input {
  flex: 1;
  background: none;
  border: none;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  padding: 4px 0;
}

.title-input::placeholder {
  color: var(--text-tertiary);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.toolbar-btn:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}

.toolbar-btn.active {
  background: var(--accent-primary);
  color: white;
}

.toolbar-btn svg {
  width: 14px;
  height: 14px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--border-default);
  margin: 0 4px;
}

.save-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--accent-success);
  color: white;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  transition: opacity var(--transition-fast);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.save-btn svg {
  width: 14px;
  height: 14px;
}

.save-btn .spin {
  animation: spin 1s linear infinite;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.delete-btn:hover {
  background: var(--accent-danger);
  color: white;
}

.delete-btn svg {
  width: 16px;
  height: 16px;
}

.editor-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-toolbar-inline {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-subtle);
}

.format-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 28px;
  background: none;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.format-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.format-btn svg {
  width: 16px;
  height: 16px;
}

.toolbar-sep {
  width: 1px;
  height: 18px;
  background: var(--border-default);
  margin: 0 6px;
}

.content-input {
  flex: 1;
  width: 100%;
  padding: 20px;
  background: var(--bg-primary);
  border: none;
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  line-height: 1.7;
  resize: none;
}

.content-input::placeholder {
  color: var(--text-tertiary);
}

.markdown-preview {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  color: var(--text-primary);
  line-height: 1.7;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3) {
  margin: 20px 0 12px 0;
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-preview :deep(h1) { font-size: 1.75em; }
.markdown-preview :deep(h2) { font-size: 1.5em; }
.markdown-preview :deep(h3) { font-size: 1.25em; }

.markdown-preview :deep(p) {
  margin: 12px 0;
}

.markdown-preview :deep(code) {
  background: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 0.9em;
}

.markdown-preview :deep(pre) {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-preview :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-preview :deep(blockquote) {
  border-left: 4px solid var(--accent-primary);
  padding-left: 16px;
  margin: 12px 0;
  color: var(--text-secondary);
}

.markdown-preview :deep(ul),
.markdown-preview :deep(ol) {
  padding-left: 24px;
  margin: 12px 0;
}

.markdown-preview :deep(a) {
  color: var(--accent-primary);
}

.markdown-preview :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-md);
}

.markdown-preview :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.markdown-preview :deep(th),
.markdown-preview :deep(td) {
  border: 1px solid var(--border-default);
  padding: 8px 12px;
  text-align: left;
}

.markdown-preview :deep(th) {
  background: var(--bg-secondary);
}
</style>