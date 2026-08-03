/**
 * 解析后端 SSE 中的知识库引用前缀：
 * __CITATIONS__[{"documentId":"...","snippet":"..."}]
 */
export function parseCitationsChunk(raw) {
  const text = String(raw ?? '')
  const marker = '__CITATIONS__'
  const idx = text.indexOf(marker)
  if (idx < 0) {
    return { citations: null, rest: text }
  }

  const after = text.slice(idx + marker.length).trimStart()
  let jsonText = after
  let rest = ''

  // JSON 数组结束后可能还有正文
  if (after.startsWith('[')) {
    let depth = 0
    let end = -1
    for (let i = 0; i < after.length; i++) {
      const ch = after[i]
      if (ch === '[') depth += 1
      if (ch === ']') {
        depth -= 1
        if (depth === 0) {
          end = i
          break
        }
      }
    }
    if (end >= 0) {
      jsonText = after.slice(0, end + 1)
      rest = after.slice(end + 1).replace(/^\n/, '')
    }
  }

  try {
    const parsed = JSON.parse(jsonText)
    const citations = Array.isArray(parsed) ? parsed : null
    return { citations, rest }
  } catch {
    return { citations: null, rest: text }
  }
}

/** 是否为「参考文档」页脚（已有引用卡片时忽略） */
export function isCitationFooter(text) {
  return /^——\s*参考文档/.test(String(text ?? '').trim())
}
