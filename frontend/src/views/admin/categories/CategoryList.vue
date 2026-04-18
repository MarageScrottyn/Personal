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
    
    <!-- 新增/编辑分类对话框 -->
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
    
    <!-- 确认删除对话框 -->
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

// 计算可作为父分类的列表（排除当前编辑的分类）
const parentCategories = computed(() => {
  return categories.value.filter(cat => {
    if (editingCategory.value) {
      return cat.id !== editingCategory.value.id
    }
    return true
  })
})

// 获取分类列表
const fetchCategories = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/categories/')
    categories.value = response.data
  } catch (error) {
    console.error('获取分类失败:', error)
  } finally {
    loading.value = false
  }
}

// 打开编辑表单
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

// 取消表单
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

// 提交分类表单
const handleCategorySubmit = async () => {
  try {
    loading.value = true
    
    if (showEditForm.value && editingCategory.value) {
      await apiClient.put(`/admin/categories/${editingCategory.value.slug}/update/`, categoryForm.value)
    } else {
      await apiClient.post('/admin/categories/create/', categoryForm.value)
    }
    
    cancelCategoryForm()
    // 重新获取分类列表
    await fetchCategories()
  } catch (error) {
    console.error('保存分类失败:', error)
  } finally {
    loading.value = false
  }
}

// 确认删除
const confirmDelete = (category) => {
  categoryToDelete.value = category
  showDeleteConfirm.value = true
}

// 取消删除
const cancelDelete = () => {
  showDeleteConfirm.value = false
  categoryToDelete.value = null
}

// 删除分类
const deleteCategory = async () => {
  if (!categoryToDelete.value) return
  
  try {
    await apiClient.delete(`/admin/categories/${categoryToDelete.value.slug}/delete/`)
    showDeleteConfirm.value = false
    // 重新获取分类列表
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
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.create-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 18px;
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
  background: #f9f9f9;
  border-radius: 12px;
  border: 2px dashed #ddd;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
  margin: 0 0 20px 0;
}

.category-table {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
}

.category-table table {
  width: 100%;
  border-collapse: collapse;
}

.category-table th,
.category-table td {
  padding: 15px 20px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.category-table th {
  background: #f5f5f5;
  font-weight: bold;
  color: #333;
}

.category-table tr:hover {
  background: #f9f9f9;
}

.action-btn {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  margin-right: 8px;
  transition: all 0.3s ease;
}

.action-btn.edit {
  background: #e3f2fd;
  color: #1976d2;
}

.action-btn.edit:hover {
  background: #bbdefb;
}

.action-btn.delete {
  background: #ffebee;
  color: #e53935;
}

.action-btn.delete:hover {
  background: #ffcdd2;
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
  background: white;
  border-radius: 12px;
  padding: 30px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
}

.dialog-content h3,
.confirm-dialog h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0 0 20px 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-group select {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
  background: white;
  cursor: pointer;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #4facfe;
  box-shadow: 0 0 0 2px rgba(79, 172, 254, 0.1);
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
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.3s ease;
}

.btn.cancel {
  background: #f5f5f5;
  color: #333;
}

.btn.cancel:hover {
  background: #e0e0e0;
}

.btn.submit {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
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
  background: #e53935;
  color: white;
}

.btn.delete:hover {
  background: #c62828;
}

.confirm-dialog p {
  font-size: 16px;
  color: #666;
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
    border-radius: 8px;
  }
  
  .category-table table {
    min-width: 400px;
  }
  
  .category-table th,
  .category-table td {
    padding: 12px 15px;
    font-size: 14px;
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
    font-size: 16px;
  }
  
  .form-group {
    margin-bottom: 15px;
  }
  
  .form-group input {
    padding: 10px;
    font-size: 14px;
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
    font-size: 14px;
  }
  
  .create-btn {
    padding: 10px 20px;
    font-size: 14px;
  }
}
</style>