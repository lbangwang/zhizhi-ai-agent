import axios from 'axios'
import { resolveApiUrl } from './config.js'
import { clearAuth, getToken } from '../utils/auth.js'

const request = axios.create({
  baseURL: resolveApiUrl('/api'),
  timeout: 60000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearAuth()
      const path = window.location.pathname
      if (path !== '/login') {
        const redirect = encodeURIComponent(path + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    return Promise.reject(error)
  },
)

export default request
