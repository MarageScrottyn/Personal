import { defineBoot } from '#q-app/wrappers'
import { createI18n } from 'vue-i18n'
import messages from 'src/i18n'

export default defineBoot(({ app }) => {
  const savedLocale = localStorage.getItem('locale') || 'zh-CN'

  const i18n = createI18n({
    legacy: false,
    locale: savedLocale,
    globalInjection: true,
    messages,
  })

  app.use(i18n)
})