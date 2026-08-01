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

/** 会话列表（当前登录用户；可按 agentType 过滤） */
export async function listConversations({ agentType } = {}) {
  const params = {}
  if (agentType) params.agentType = agentType
  return unwrap(await request.get('/conversations', { params }))
}

/** 创建会话 */
export async function createConversation(payload) {
  return unwrap(await request.post('/conversations', payload))
}

/** 按 chatId 查询会话 */
export async function getConversation(chatId) {
  return unwrap(await request.get(`/conversations/${encodeURIComponent(chatId)}`))
}

/** 更新会话 */
export async function updateConversation(chatId, payload) {
  return unwrap(await request.put(`/conversations/${encodeURIComponent(chatId)}`, payload))
}

/** 删除会话（逻辑删除） */
export async function deleteConversation(chatId) {
  return unwrap(await request.delete(`/conversations/${encodeURIComponent(chatId)}`))
}

/** 追加消息 */
export async function addMessage(chatId, { role, content, metadata } = {}) {
  return unwrap(
    await request.post(`/conversations/${encodeURIComponent(chatId)}/messages`, {
      role,
      content,
      metadata,
    }),
  )
}

/** 消息列表 */
export async function listMessages(chatId) {
  return unwrap(await request.get(`/conversations/${encodeURIComponent(chatId)}/messages`))
}

/**
 * 确保会话存在：已存在则返回，否则创建。
 */
export async function ensureConversation({ chatId, agentType, title, model }) {
  try {
    return await getConversation(chatId)
  } catch {
    return createConversation({
      chatId,
      agentType,
      title: title || '新对话',
      model,
    })
  }
}
