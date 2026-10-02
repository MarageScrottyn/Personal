const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/comics' },
      {
        path: 'comics',
        component: () => import('pages/comics/ComicListView.vue')
      },
      {
        path: 'comics/:slug',
        component: () => import('pages/comics/ComicDetailView.vue')
      },
      {
        path: 'comics/:comicSlug/read/:chapterId',
        component: () => import('pages/comics/ComicReadView.vue')
      },
      {
        path: 'videos',
        component: () => import('pages/videos/VideoListView.vue')
      },
      {
        path: 'videos/:slug',
        component: () => import('pages/videos/VideoDetailView.vue')
      },
      {
        path: 'notes',
        component: () => import('pages/notes/NoteListView.vue')
      },
      {
        path: 'cloud',
        component: () => import('pages/cloud/CloudDriveView.vue')
      },
      {
        path: 'heart',
        component: () => import('pages/heart/HeartView.vue')
      },
      {
        path: 'brain',
        component: () => import('pages/BrainControlView.vue')
      },
      {
        path: 'profile',
        component: () => import('pages/profile/UserProfileView.vue')
      },
      {
        path: 'profile/settings',
        component: () => import('pages/profile/AccountSettings.vue')
      },
      {
        path: 'profile/security',
        component: () => import('pages/profile/SecuritySettings.vue')
      },
      {
        path: 'profile/privacy',
        component: () => import('pages/profile/PrivacySettings.vue')
      },
      {
        path: 'categories/:slug',
        component: () => import('pages/categories/CategoryBrowseView.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('layouts/AdminLayout.vue'),
    children: [
      {
        path: '',
        component: () => import('pages/admin/AdminDashboard.vue')
      },
      {
        path: 'comics',
        component: () => import('pages/admin/comics/ComicList.vue')
      },
      {
        path: 'comics/create',
        component: () => import('pages/admin/comics/ComicForm.vue')
      },
      {
        path: 'comics/edit/:slug',
        component: () => import('pages/admin/comics/ComicForm.vue')
      },
      {
        path: 'videos',
        component: () => import('pages/admin/videos/VideoList.vue')
      },
      {
        path: 'videos/create',
        component: () => import('pages/admin/videos/VideoForm.vue')
      },
      {
        path: 'videos/edit/:slug',
        component: () => import('pages/admin/videos/VideoForm.vue')
      },
      {
        path: 'categories',
        component: () => import('pages/admin/categories/CategoryList.vue')
      }
    ]
  },
  {
    path: '/settings',
    component: () => import('layouts/SettingsLayout.vue'),
    children: [
      { path: '', redirect: '/settings/account' },
      {
        path: 'account',
        component: () => import('pages/profile/AccountSettings.vue')
      },
      {
        path: 'security',
        component: () => import('pages/profile/SecuritySettings.vue')
      },
      {
        path: 'privacy',
        component: () => import('pages/profile/PrivacySettings.vue')
      },
      {
        path: 'appearance',
        component: () => import('pages/settings/AppearanceSettings.vue')
      },
      {
        path: 'dynamicIsland',
        component: () => import('pages/settings/DynamicIslandSettingsView.vue')
      },
      {
        path: 'taskbar',
        component: () => import('pages/settings/TaskBarSettings.vue')
      },
      {
        path: 'notifications',
        component: () => import('pages/settings/NotificationsSettings.vue')
      },
      {
        path: 'language',
        component: () => import('pages/settings/LanguageSettings.vue')
      },
      {
        path: 'general',
        component: () => import('pages/settings/GeneralSettings.vue')
      },
      {
        path: 'devTools',
        component: () => import('pages/settings/DevToolsSettings.vue')
      }
    ]
  },
  {
    path: '/dynamic-island',
    component: () => import('pages/DynamicIslandView.vue')
  },
  {
    path: '/taskbar',
    component: () => import('pages/TaskBarView.vue')
  },
  {
    path: '/start-menu',
    component: () => import('src/windows/StartMenuWindow.vue')
  },
  {
    path: '/quick-settings',
    component: () => import('src/windows/QuickSettingsWindow.vue')
  },
  {
    path: '/notification-center',
    component: () => import('src/windows/NotificationCenterWindow.vue')
  },
  {
    path: '/system-tray',
    component: () => import('src/windows/SystemTrayWindow.vue')
  },
  {
    path: '/window-preview',
    component: () => import('src/windows/WindowPreviewWindow.vue')
  },
  {
    path: '/login',
    component: () => import('pages/LoginView.vue')
  },
  {
    path: '/register',
    component: () => import('pages/RegisterView.vue')
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
