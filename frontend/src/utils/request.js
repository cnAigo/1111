import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    let msg = '请求失败'
    if (error.response) {
      const data = error.response.data
      msg = data?.msg || data?.message || `HTTP ${error.response.status}`
    } else if (error.code === 'ECONNABORTED') {
      msg = '请求超时'
    } else if (!window.navigator.onLine) {
      msg = '网络连接已断开'
    }
    ElMessage.error(msg)
    window.dispatchEvent(new CustomEvent('api:error', { detail: { msg, error } }))
    return Promise.reject(error)
  }
)

export default request
