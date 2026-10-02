import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: localStorage.getItem('theme') || 'dark'
  }),

  getters: {
    isDark: (state) => state.theme === 'dark',
    isLight: (state) => state.theme === 'light'
  },

  actions: {
    setTheme(theme) {
      this.theme = theme
      localStorage.setItem('theme', theme)
      this.applyTheme()
    },

    toggleTheme() {
      const newTheme = this.theme === 'dark' ? 'light' : 'dark'
      this.setTheme(newTheme)
    },

    applyTheme() {
      document.documentElement.setAttribute('data-theme', this.theme)
      
      // 更新 CSS 变量
      this.updateCssVariables()
    },

    updateCssVariables() {
      const root = document.documentElement
      
      if (this.theme === 'dark') {
        root.style.setProperty('--bg-primary', '#0f0f0f')
        root.style.setProperty('--bg-secondary', '#1a1a1a')
        root.style.setProperty('--bg-tertiary', '#242424')
        root.style.setProperty('--bg-elevated', '#2a2a2a')
        root.style.setProperty('--text-primary', '#ffffff')
        root.style.setProperty('--text-secondary', '#a1a1a1')
        root.style.setProperty('--text-tertiary', '#6b6b6b')
        root.style.setProperty('--border-subtle', '#2d2d2d')
        root.style.setProperty('--border-default', '#3d3d3d')
      } else {
        root.style.setProperty('--bg-primary', '#ffffff')
        root.style.setProperty('--bg-secondary', '#f5f5f5')
        root.style.setProperty('--bg-tertiary', '#e8e8e8')
        root.style.setProperty('--bg-elevated', '#ffffff')
        root.style.setProperty('--text-primary', '#1a1a1a')
        root.style.setProperty('--text-secondary', '#666666')
        root.style.setProperty('--text-tertiary', '#999999')
        root.style.setProperty('--border-subtle', '#e0e0e0')
        root.style.setProperty('--border-default', '#cccccc')
      }
    },

    initTheme() {
      this.applyTheme()
    }
  }
})