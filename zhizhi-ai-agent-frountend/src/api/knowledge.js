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

function appendSplitParams(form, params = {}) {
  const {
    splitStrategy,
    chunkTokenSize,
    paragraphMaxChars,
    paragraphMinMergeChars,
    minChunkLengthToEmbed,
    maxNumChunks,
  } = params
  if (splitStrategy) form.append('splitStrategy', splitStrategy)
  if (chunkTokenSize != null && chunkTokenSize !== '') form.append('chunkTokenSize', String(chunkTokenSize))
  if (paragraphMaxChars != null && paragraphMaxChars !== '') {
    form.append('paragraphMaxChars', String(paragraphMaxChars))
  }
  if (paragraphMinMergeChars != null && paragraphMinMergeChars !== '') {
    form.append('paragraphMinMergeChars', String(paragraphMinMergeChars))
  }
  if (minChunkLengthToEmbed != null && minChunkLengthToEmbed !== '') {
    form.append('minChunkLengthToEmbed', String(minChunkLengthToEmbed))
  }
  if (maxNumChunks != null && maxNumChunks !== '') form.append('maxNumChunks', String(maxNumChunks))
}

/** 切片预览（不入库） */
export async function previewSplit(file, params = {}) {
  const form = new FormData()
  form.append('file', file)
  appendSplitParams(form, params)
  return unwrap(
    await request.post('/knowledge/documents/preview-split', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

/** 上传知识库文档（.md / .txt / Word），可带切分参数 */
export async function uploadDocument(file, title, params = {}) {
  const form = new FormData()
  form.append('file', file)
  if (title) form.append('title', title)
  appendSplitParams(form, params)
  return unwrap(
    await request.post('/knowledge/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      // 入库需批量 embedding，大文档可能超过默认 60s
      timeout: 180000,
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

/** 已入库文档切片列表 */
export async function listChunks(id) {
  return unwrap(await request.get(`/knowledge/documents/${encodeURIComponent(id)}/chunks`))
}

/** 删除文档 */
export async function deleteDocument(id) {
  return unwrap(await request.delete(`/knowledge/documents/${encodeURIComponent(id)}`))
}

/** 相似度检索 */
export async function retrieveKnowledge({ query, topK } = {}) {
  return unwrap(await request.post('/knowledge/retrieve', { query, topK }))
}
