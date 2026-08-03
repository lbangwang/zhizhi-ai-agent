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

/** 上传知识库文档（.md / .txt） */
export async function uploadDocument(file, title) {
  const form = new FormData()
  form.append('file', file)
  if (title) form.append('title', title)
  return unwrap(
    await request.post('/knowledge/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

/** 文档列表 */
export async function listDocuments() {
  return unwrap(await request.get('/knowledge/documents'))
}

/** 文档详情 */
export async function getDocument(id) {
  return unwrap(await request.get(`/knowledge/documents/${encodeURIComponent(id)}`))
}

/** 删除文档 */
export async function deleteDocument(id) {
  return unwrap(await request.delete(`/knowledge/documents/${encodeURIComponent(id)}`))
}

/** 相似度检索 */
export async function retrieveKnowledge({ query, topK } = {}) {
  return unwrap(await request.post('/knowledge/retrieve', { query, topK }))
}
