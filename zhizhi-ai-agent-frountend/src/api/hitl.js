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

export async function approveHitl(approvalId) {
  return unwrap(await request.post(`/hitl/${encodeURIComponent(approvalId)}/approve`))
}

export async function rejectHitl(approvalId) {
  return unwrap(await request.post(`/hitl/${encodeURIComponent(approvalId)}/reject`))
}
