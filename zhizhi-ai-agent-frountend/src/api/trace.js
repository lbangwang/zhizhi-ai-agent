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

export async function listTraces({ chatId, limit } = {}) {
  return unwrap(
    await request.get('/traces', {
      params: {
        ...(chatId ? { chatId } : {}),
        ...(limit ? { limit } : {}),
      },
    }),
  )
}

export async function getTrace(traceId) {
  return unwrap(await request.get(`/traces/${encodeURIComponent(traceId)}`))
}

export async function getTraceStats() {
  return unwrap(await request.get('/traces/stats/summary'))
}
