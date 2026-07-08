import { defineStore } from 'pinia'
import { ref } from 'vue'
import i18n from '@/locales'

export const useAppStore = defineStore('app', () => {
  const language = ref(localStorage.getItem('app-lang') || 'zh-cn')

  const setLanguage = (lang) => {
    language.value = lang
    i18n.global.locale.value = lang
    localStorage.setItem('app-lang', lang)
    document.querySelector('html')?.setAttribute('lang', lang)
  }

  return { language, setLanguage }
})
