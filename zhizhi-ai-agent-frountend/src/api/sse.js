import { resolveApiUrl } from './config.js'
import { authHeader, clearAuth } from '../utils/auth.js'

/**
 * 通过 SSE 流式请求后端接口
 * 按完整 SSE 事件回调（同一事件内多行 data 会用换行拼接）
 * @param {string} url - 请求地址
 * @param {Object} params - 查询参数
 * @param {(chunk: string) => void} onMessage - 收到完整事件数据时的回调
 * @param {AbortSignal} [signal] - 可选的取消信号
 */
export async function fetchSSE(url, params, onMessage, signal) {
  const searchParams = new URLSearchParams(params)
  const fullUrl = `${resolveApiUrl(url)}?${searchParams.toString()}`

  const response = await fetch(fullUrl, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      ...authHeader(),
    },
    signal,
  })

  if (response.status === 401) {
    clearAuth()
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
    throw new Error('未登录或登录已过期')
  }

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let dataLines = []

  function flushEvent() {
    if (dataLines.length === 0) return
    const data = dataLines.join('\n')
    dataLines = []
    if (data && data !== '[DONE]') {
      onMessage(data)
    }
  }

  function handleLine(line) {
    // SSE 事件以空行分隔
    if (line === '' || line === '\r') {
      flushEvent()
      return
    }

    const trimmed = line.replace(/\r$/, '')

    if (trimmed.startsWith('data:')) {
      dataLines.push(trimmed.slice(5).replace(/^ /, ''))
      return
    }

    if (
      trimmed.startsWith('event:') ||
      trimmed.startsWith('id:') ||
      trimmed.startsWith('retry:') ||
      trimmed.startsWith(':')
    ) {
      return
    }

    // 非标准纯文本流，直接作为一次输出
    if (trimmed) {
      onMessage(trimmed)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      handleLine(line)
    }
  }

  if (buffer) {
    handleLine(buffer)
  }
  flushEvent()
}
