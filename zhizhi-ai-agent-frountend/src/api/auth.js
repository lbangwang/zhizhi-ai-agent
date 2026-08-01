import request from './request.js'
import { clearAuth, setAuth } from '../utils/auth.js'

function unwrap(response) {
  const body = response?.data
  if (!body || typeof body !== 'object') {
    throw new Error('接口返回异常')
  }
  if (body.code !== 0) {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

export async function register({ username, password, nickname }) {
  const data = unwrap(await request.post('/auth/register', { username, password, nickname }))
  setAuth({ token: data.token, user: data.user })
  return data
}

export async function login({ username, password }) {
  const data = unwrap(await request.post('/auth/login', { username, password }))
  setAuth({ token: data.token, user: data.user })
  return data
}

export async function logout() {
  try {
    await request.post('/auth/logout')
  } catch {
    // ignore
  } finally {
    clearAuth()
  }
}

export async function fetchMe() {
  return unwrap(await request.get('/auth/me'))
}
