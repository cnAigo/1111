import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref({ id: 1, username: 'admin' })
  const accessToken = ref('')
  const refreshToken = ref('')
  const tokenExpiresAt = ref(0)

  async function fetchProfile() {
    user.value = { id: 1, username: 'admin' }
    return user.value
  }

  async function initAuth() { /* noop */ }
  async function login() {}
  async function logout() {}

  return { user, accessToken, refreshToken, tokenExpiresAt, fetchProfile, initAuth, login, logout }
})
