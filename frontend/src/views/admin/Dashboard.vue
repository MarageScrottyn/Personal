<template>
  <div class="dashboard">
    <div class="dashboard-overview">
      <h2>系统概览</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon comics">📚</div>
          <div class="stat-content">
            <div class="stat-value">{{ comicCount }}</div>
            <div class="stat-label">漫画数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon videos">🎥</div>
          <div class="stat-content">
            <div class="stat-value">{{ videoCount }}</div>
            <div class="stat-label">视频数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon categories">📁</div>
          <div class="stat-content">
            <div class="stat-value">{{ categoryCount }}</div>
            <div class="stat-label">分类数量</div>
          </div>
        </div>
      </div>
    </div>
    <div class="dashboard-recent">
      <h2>最近更新</h2>
      <div class="recent-items">
        <div v-if="recentComics.length === 0 && recentVideos.length === 0" class="empty-state">
          <span class="empty-icon">📭</span>
          <p>暂无更新内容</p>
        </div>
        <div v-for="comic in recentComics" :key="comic.id" class="recent-item comic">
          <div class="item-icon">📚</div>
          <div class="item-info">
            <h3>{{ comic.title }}</h3>
            <p>{{ formatDate(comic.created_at) }}</p>
          </div>
          <router-link to="/admin/comics/edit/${comic.slug}" class="item-action">
            编辑
          </router-link>
        </div>
        <div v-for="video in recentVideos" :key="video.id" class="recent-item video">
          <div class="item-icon">🎥</div>
          <div class="item-info">
            <h3>{{ video.title }}</h3>
            <p>{{ formatDate(video.created_at) }}</p>
          </div>
          <router-link to="/admin/videos/edit/${video.slug}" class="item-action">
            编辑
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '../../api/client'

const comicCount = ref(0)
const videoCount = ref(0)
const categoryCount = ref(0)
const recentComics = ref([])
const recentVideos = ref([])

const fetchStats = async () => {
  try {
    const [comicsRes, videosRes, categoriesRes] = await Promise.all([
      apiClient.get('/comics/'),
      apiClient.get('/videos/'),
      apiClient.get('/categories/')
    ])
    
    comicCount.value = comicsRes.data.length
    videoCount.value = videosRes.data.length
    categoryCount.value = categoriesRes.data.length
    
    // 获取最近更新的内容
    recentComics.value = comicsRes.data
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
      .slice(0, 3)
    
    recentVideos.value = videosRes.data
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
      .slice(0, 3)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.dashboard-overview h2,
.dashboard-recent h2 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 32px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.comics {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-icon.videos {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-icon.categories {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.recent-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.recent-item {
  background: white;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.recent-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.recent-item .item-icon {
  font-size: 24px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recent-item.comic .item-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.recent-item.video .item-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.item-info {
  flex: 1;
}

.item-info h3 {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 5px 0;
}

.item-info p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.item-action {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.item-action:hover {
  opacity: 0.9;
  transform: translateY(-2px);
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
  margin: 0;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stat-card {
    flex-direction: column;
    text-align: center;
  }
  
  .recent-item {
    flex-direction: column;
    text-align: center;
  }
}
</style>