/**
 * 后端 API 根地址（不含尾斜杠）。
 * 本地/生产可通过 .env 的 VITE_API_BASE 指向云端，例如：
 * VITE_API_BASE=https://zhizhi-ai-agent-xxx.sh.run.tcloudbase.com
 * 未配置时走相对路径 /api（由 Vite 或 Nginx 代理）。
 */
export const API_BASE = String(import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

/** 把 /api/... 相对路径解析为可请求的绝对/相对 URL */
export function resolveApiUrl(path) {
  if (!path) return path
  if (/^https?:\/\//i.test(path)) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  return API_BASE ? `${API_BASE}${normalized}` : normalized
}
