import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/comics'
        },
        {
          path: 'comics',
          name: 'comics',
          component: () => import('../views/comics/ComicListView.vue')
        },
        {
          path: 'comics/:slug',
          name: 'comic-detail',
          component: () => import('../views/comics/ComicDetailView.vue')
        },
        {
          path: 'comics/:comicSlug/read/:chapterId',
          name: 'comic-read',
          component: () => import('../views/comics/ComicReadView.vue')
        },
        {
          path: 'videos',
          name: 'videos',
          component: () => import('../views/videos/VideoListView.vue')
        },
        {
          path: 'videos/:slug',
          name: 'video-detail',
          component: () => import('../views/videos/VideoDetailView.vue')
        },
        {
          path: 'notes',
          name: 'notes',
          component: () => import('../views/notes/NoteListView.vue')
        },
        {
          path: 'cloud',
          name: 'cloud',
          component: () => import('../views/cloud/CloudDriveView.vue')
        },
        {
          path: 'categories/:slug',
          name: 'category-browse',
          component: () => import('../views/categories/CategoryBrowseView.vue')
        }
      ]
    },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('../views/admin/Dashboard.vue')
        },
        {
          path: 'comics',
          name: 'admin-comics',
          component: () => import('../views/admin/comics/ComicList.vue')
        },
        {
          path: 'comics/create',
          name: 'admin-comic-create',
          component: () => import('../views/admin/comics/ComicForm.vue')
        },
        {
          path: 'comics/edit/:slug',
          name: 'admin-comic-edit',
          component: () => import('../views/admin/comics/ComicForm.vue')
        },
        {
          path: 'videos',
          name: 'admin-videos',
          component: () => import('../views/admin/videos/VideoList.vue')
        },
        {
          path: 'videos/create',
          name: 'admin-video-create',
          component: () => import('../views/admin/videos/VideoForm.vue')
        },
        {
          path: 'videos/edit/:slug',
          name: 'admin-video-edit',
          component: () => import('../views/admin/videos/VideoForm.vue')
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('../views/admin/categories/CategoryList.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/comics')
  } else if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    next('/comics')
  } else {
    next()
  }
})

export default router