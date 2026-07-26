/**
 * 生成唯一聊天室 ID
 */
export function generateChatId() {
  return `chat_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
}
