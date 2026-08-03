import request from './request.js'

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

/** 当前会话产物列表 */
export async function listArtifacts({ chatId } = {}) {
  return unwrap(
    await request.get('/artifacts', {
      params: chatId ? { chatId } : undefined,
    }),
  )
}

/** 下载产物（带鉴权，触发浏览器保存） */
export async function downloadArtifact(id, fileName) {
  const response = await request.get(`/artifacts/${encodeURIComponent(id)}/download`, {
    responseType: 'blob',
  })
  const blob = response.data
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName || 'artifact'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

function formatBytes(n) {
  if (n == null || Number.isNaN(Number(n))) return ''
  const v = Number(n)
  if (v < 1024) return `${v} B`
  if (v < 1024 * 1024) return `${(v / 1024).toFixed(1)} KB`
  return `${(v / (1024 * 1024)).toFixed(1)} MB`
}

export { formatBytes }
