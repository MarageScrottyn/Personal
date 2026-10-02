<template>
  <div class="language-switcher">
    <q-btn-dropdown
      flat
      dense
      no-caps
      :label="currentLanguage"
      class="lang-btn"
    >
      <q-list>
        <q-item
          v-for="lang in languages"
          :key="lang.code"
          clickable
          v-close-popup
          @click="switchLanguage(lang.code)"
        >
          <q-item-section>
            <q-item-label>{{ lang.name }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const languages = [
  { code: 'zh-CN', name: '中文' },
  { code: 'en-US', name: 'English' }
]

const currentLanguage = computed(() => {
  const lang = languages.find(l => l.code === locale.value)
  return lang ? lang.name : '中文'
})

const switchLanguage = (code) => {
  locale.value = code
  localStorage.setItem('locale', code)
}
</script>

<style scoped>
.language-switcher {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
}

.lang-btn {
  color: var(--flora-stem);
  font-size: 0.875rem;
}

.lang-btn:hover {
  color: var(--flora-leaf);
}
</style>