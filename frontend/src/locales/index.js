import { createI18n } from 'vue-i18n'
import zhCn from './lang/zh-cn'

const defaultLang = localStorage.getItem('app-lang') || 'zh-cn'

const i18n = createI18n({
  legacy: false,
  locale: defaultLang,
  fallbackLocale: 'zh-cn',
  globalInjection: true,
  messages: {
    'zh-cn': zhCn,
  },
  missingWarn: false,
  fallbackWarn: false,
})

export default i18n
