<template>
  <div class="category-list">
    <div class="list-header">
      <h2>分类管理</h2>
      <button @click="showAddForm = true" class="create-btn">
        <span class="btn-icon">➕</span>
        <span>新增分类</span>
      </button>
    </div>
    <div class="list-content">
      <div v-if="loading" class="loading">
        <span class="loading-icon">⏳</span>
        <p>加载中...</p>
      </div>
      <div v-else-if="categories.length === 0" class="empty-state">
        <span class="empty-icon">📁</span>
        <p>暂无分类</p>
        <button @click="showAddForm = true" class="create-btn">
          新增分类
        </button>
      </div>
      <div v-else class="category-table">
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>Slug</th>
              <th>父分类</th>
              <th>权限级别</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="category in categories" :key="category.id">
              <td>{{ category.name }}</td>
              <td>{{ category.slug }}</td>
              <td>{{ category.parent_name || '无' }}</td>
              <td>{{ category.permission_level === 'regular' ? '普通权限' : '特殊权限' }}</td>
              <td>
                <button @click="editCategory(category)" class="action-btn edit">
                  编辑
                </button>
                <button @click="confirmDelete(category)" class="action-btn delete">
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showAddForm || showEditForm" class="form-dialog">
      <div class="dialog-content">
        <h3>{{ showEditForm ? '编辑分类' : '新增分类' }}</h3>
        <form @submit.prevent="handleCategorySubmit">
          <div class="form-group">
            <label for="category-name">分类名称</label>
            <input
              type="text"
              id="category-name"
              v-model="categoryForm.name"
              required
              placeholder="请输入分类名称"
            />
          </div>
          <div class="form-group">
            <label for="category-slug">Slug</label>
            <input
              type="text"
              id="category-slug"
              v-model="categoryForm.slug"
              required
              placeholder="请输入slug (小写字母和连字符)"
            />
          </div>
          <div class="form-group">
            <label for="category-parent">父分类</label>
            <select
              id="category-parent"
              v-model="categoryForm.parent"
              placeholder="选择父分类"
            >
              <option value="">无 (顶级分类)</option>
              <option
                v-for="cat in parentCategories"
                :key="cat.id"
                :value="cat.id"
                :disabled="editingCategory && cat.id === editingCategory.id"
              >
                {{ cat.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label for="category-permission">权限级别</label>
            <select
              id="category-permission"
              v-model="categoryForm.permission_level"
              required
            >
              <option value="regular">普通权限</option>
              <option value="special">特殊权限</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" @click="cancelCategoryForm" class="btn cancel">取消</button>
            <button type="submit" class="btn submit" :disabled="loading">
              {{ loading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showDeleteConfirm" class="delete-confirm">
      <div class="confirm-dialog">
        <h3>确认删除</h3>
        <p>确定要删除分类 <strong>{{ categoryToDelete?.name }}</strong> 吗？</p>
        <div class="confirm-actions">
          <button @click="cancelDelete" class="btn cancel">取消</button>
          <button @click="deleteCategory" class="btn delete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import apiClient from '../../../api/client'

const categories = ref([])
const loading = ref(true)
const showAddForm = ref(false)
const showEditForm = ref(false)
const showDeleteConfirm = ref(false)
const categoryToDelete = ref(null)
const categoryForm = ref({
  name: '',
  slug: '',
  parent: '',
  permission_level: 'regular'
})
const editingCategory = ref(null)

const parentCategories = computed(() => {
  return categories.value.filter(cat => {
    if (editingCategory.value) {
      return cat.id !== editingCategory.value.id
    }
    return true
  })
})

const fetchCategories = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/categories/')
    categories.value = response.data.results || response.data
  } catch (error) {
    console.error('获取分类失败:', error)
  } finally {
    loading.value = false
  }
}

const editCategory = (category) => {
  editingCategory.value = category
  categoryForm.value = {
    name: category.name,
    slug: category.slug,
    parent: category.parent || '',
    permission_level: category.permission_level
  }
  showEditForm.value = true
}

const cancelCategoryForm = () => {
  showAddForm.value = false
  showEditForm.value = false
  editingCategory.value = null
  categoryForm.value = {
    name: '',
    slug: '',
    parent: '',
    permission_level: 'regular'
  }
}

const handleCategorySubmit = async () => {
  try {
    loading.value = true

    if (showEditForm.value && editingCategory.value) {
      await apiClient.put(`/admin/categories/${editingCategory.value.slug}/update/`, categoryForm.value)
    } else {
      await apiClient.post('/admin/categories/create/', categoryForm.value)
    }

    cancelCategoryForm()
    await fetchCategories()
  } catch (error) {
    console.error('保存分类失败:', error)
  } finally {
    loading.value = false
  }
}

const confirmDelete = (category) => {
  categoryToDelete.value = category
  showDeleteConfirm.value = true
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  categoryToDelete.value = null
}

const deleteCategory = async () => {
  if (!categoryToDelete.value) return

  try {
    await apiClient.delete(`/admin/categories/${categoryToDelete.value.slug}/delete/`)
    showDeleteConfirm.value = false
    await fetchCategories()
  } catch (error) {
    console.error('删除分类失败:', error)
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.category-list {
  position: relative;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.list-header h2 {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--flora-stem) 0%, var(--flora-stem-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-family: var(--font-body);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.create-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px var(--flora-shadow);
}

.btn-icon {
  font-size: 1.125rem;
}

.loading {
  text-align: center;
  padding: 60px 20px;
}

.loading-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  border: 2px dashed var(--flora-base-dark);
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
}

.empty-state p {
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--flora-stem);
  margin: 0 0 20px 0;
}

.category-table {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: 0 4px 15px var(--flora-shadow);
}

.category-table table {
  width: 100%;
  border-collapse: collapse;
}

.category-table th,
.category-table td {
  padding: 15px 20px;
  text-align: left;
  border-bottom: 1px solid var(--flora-base-dark);
}

.category-table th {
  background: var(--flora-base);
  font-family: var(--font-heading);
  font-weight: bold;
  color: var(--flora-stem);
}

.category-table td {
  font-family: var(--font-body);
  color: var(--flora-stem);
}

.category-table tr:hover {
  background: var(--flora-base);
}

.action-btn {
  padding: 6px 12px;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  margin-right: 8px;
  transition: all var(--transition-fast);
}

.action-btn.edit {
  background: var(--flora-base);
  color: var(--flora-stem);
}

.action-btn.edit:hover {
  background: var(--flora-base-dark);
}

.action-btn.delete {
  background: var(--flora-base);
  color: var(--flora-bloom-dark);
}

.action-btn.delete:hover {
  background: var(--flora-base-dark);
}

.form-dialog,
.delete-confirm {
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

.dialog-content,
.confirm-dialog {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 30px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 60px var(--flora-shadow);
}

.dialog-content h3,
.confirm-dialog h3 {
  font-family: var(--font-heading);
  font-size: 1.125rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0 0 20px 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.form-group label {
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--flora-stem);
}

.form-group input {
  padding: 12px;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: border-color var(--transition-fast);
  background: var(--flora-base);
  color: var(--flora-stem);
}

.form-group select {
  padding: 12px;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: border-color var(--transition-fast);
  background: var(--flora-base);
  color: var(--flora-stem);
  cursor: pointer;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--flora-leaf);
}

.form-actions,
.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all var(--transition-fast);
}

.btn.cancel {
  background: var(--flora-base);
  color: var(--flora-stem);
}

.btn.cancel:hover {
  background: var(--flora-base-dark);
}

.btn.submit {
  background: linear-gradient(135deg, var(--flora-stem) 0%, var(--flora-stem-light) 100%);
  color: white;
}

.btn.submit:hover:not(:disabled) {
  opacity: 0.9;
}

.btn.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.delete {
  background: var(--flora-bloom-dark);
  color: white;
}

.btn.delete:hover {
  background: var(--flora-bloom);
}

.confirm-dialog p {
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--flora-stem);
  margin: 0 0 25px 0;
}

@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
    padding: 0 15px;
  }

  .list-content {
    padding: 0 15px;
  }

  .category-table {
    overflow-x: auto;
    border-radius: var(--radius-lg);
  }

  .category-table table {
    min-width: 400px;
  }

  .category-table th,
  .category-table td {
    padding: 12px 15px;
    font-size: 0.875rem;
  }

  .action-btn {
    margin-bottom: 8px;
    margin-right: 0;
    width: 100%;
    margin-bottom: 8px;
  }

  .action-btn:last-child {
    margin-bottom: 0;
  }

  .dialog-content,
  .confirm-dialog {
    padding: 20px;
    width: 95%;
  }

  .dialog-content h3,
  .confirm-dialog h3 {
    font-size: 1rem;
  }

  .form-group {
    margin-bottom: 15px;
  }

  .form-group input {
    padding: 10px;
    font-size: 0.875rem;
  }

  .form-actions,
  .confirm-actions {
    flex-direction: column;
    gap: 8px;
    margin-top: 15px;
  }

  .btn {
    width: 100%;
    padding: 10px;
  }

  .empty-state {
    padding: 40px 15px;
  }

  .empty-icon {
    font-size: 36px;
  }

  .empty-state p {
    font-size: 0.875rem;
  }

  .create-btn {
    padding: 10px 20px;
    font-size: 0.875rem;
  }
}
</style>