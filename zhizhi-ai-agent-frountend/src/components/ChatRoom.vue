<template>
  <div class="chat-room-wrapper" :data-accent="accent">
    <ParticleBackground density="light" />
    <div class="orb orb-a" aria-hidden="true" />
    <div class="orb orb-b" aria-hidden="true" />

    <div class="chat-room">
      <header class="chat-header">
        <button class="back-btn" type="button" @click="$router.push('/')">返回</button>
        <div class="header-info">
          <div class="title-row">
            <img class="app-mark" :src="aiAvatar" :alt="`${title}头像`" />
            <h1>{{ title }}</h1>
          </div>
          <span v-if="chatId" class="chat-id">会话 {{ chatId }}</span>
        </div>
      </header>

      <div ref="messagesRef" class="chat-messages">
        <div v-if="messages.length === 0" class="empty-tip">
          <img class="empty-avatar" :src="aiAvatar" :alt="`${title}头像`" />
          <p>{{ emptyTip }}</p>
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-row', msg.role === 'user' ? 'message-row-user' : 'message-row-ai']"
        >
          <div :class="['message-item', msg.role === 'user' ? 'message-user' : 'message-ai']">
            <img
              class="avatar"
              :src="msg.role === 'user' ? userAvatar : aiAvatar"
              :alt="msg.role === 'user' ? '用户头像' : `${title}头像`"
            />
            <div class="bubble">
              <p class="bubble-text">{{ msg.content }}</p>
              <span v-if="msg.loading" class="typing-cursor">|</span>
            </div>
          </div>
          <div
            v-if="msg.role === 'user' && msg.content && editingIndex !== index"
            class="user-actions"
          >
            <button
              type="button"
              class="action-btn"
              title="复制"
              aria-label="复制"
              :disabled="isLoading"
              @click="copyUserMessage(msg.content, index)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect
                  x="9"
                  y="9"
                  width="11"
                  height="11"
                  rx="2"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                />
                <path
                  d="M6 15H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                />
              </svg>
            </button>
            <button
              type="button"
              class="action-btn"
              title="修改后发送"
              aria-label="修改后发送"
              :disabled="isLoading"
              @click="editUserMessage(index)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 20h9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                />
                <path
                  d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4L16.5 3.5z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
            <span v-if="copiedIndex === index" class="copied-tip">已复制</span>
          </div>
          <div
            v-if="msg.role === 'user' && editingIndex === index"
            class="inline-edit"
            role="dialog"
            aria-label="修改问题"
          >
            <textarea
              ref="editInputRef"
              v-model="editText"
              class="edit-textarea"
              rows="3"
              :disabled="isLoading"
              @keydown.enter.exact.prevent="confirmEditAndSend"
              @keydown.esc.prevent="closeEditModal"
            />
            <div class="edit-footer">
              <button type="button" class="edit-cancel" @click="closeEditModal">取消</button>
              <button
                type="button"
                class="edit-confirm"
                :disabled="!editText.trim() || isLoading"
                @click="confirmEditAndSend"
              >
                发送
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="composer">
        <div class="composer-box">
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="chat-input"
            :placeholder="placeholder"
            :disabled="isLoading"
            rows="2"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="composer-actions">
            <button
              v-if="isLoading"
              class="stop-btn"
              type="button"
              title="停止输出"
              aria-label="停止输出"
              @click="stopMessage"
            >
              <span class="stop-square" />
            </button>
            <button
              v-else
              class="send-btn"
              type="button"
              :disabled="!inputText.trim()"
              title="发送"
              aria-label="发送"
              @click="sendMessage"
            >
              <svg class="send-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 19V5M12 5l-6 6M12 5l6 6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.4"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
          </div>
        </div>
        <p class="ai-disclaimer">内容由AI生成，请仔细斟酌</p>
      </div>
    </div>
    <SiteFooter compact />
  </div>
</template>

<script setup>
import { computed, ref, nextTick, watch } from 'vue'
import { fetchSSE } from '../api/sse.js'
import { APP_AVATARS } from '../constants/apps.js'
import SiteFooter from './SiteFooter.vue'
import ParticleBackground from './ParticleBackground.vue'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  apiUrl: {
    type: String,
    required: true,
  },
  chatId: {
    type: String,
    default: '',
  },
  emptyTip: {
    type: String,
    default: '开始对话吧，输入消息后按 Enter 发送',
  },
  placeholder: {
    type: String,
    default: '输入消息...',
  },
  /** 为 true 时，每个 SSE 事件单独一个 AI 气泡（超级智能体按步骤展示） */
  stepMode: {
    type: Boolean,
    default: false,
  },
  /** 应用主题色：love | agent */
  accent: {
    type: String,
    default: 'agent',
    validator: (value) => ['love', 'agent'].includes(value),
  },
  /** AI 应用默认头像，不传则按 accent 自动匹配 */
  avatar: {
    type: String,
    default: '',
  },
  /** 停止输出接口（固定为 stopChatByZhizhiManus，勿与对话接口混淆） */
  stopApiUrl: {
    type: String,
    default: '/api/zhizhi-ai/stopChatByZhizhiManus',
  },
  /** 停止接口 type：恋爱大师 PROFESSIONAL，超级智能体 COMMON */
  stopType: {
    type: String,
    default: 'COMMON',
    validator: (value) => ['PROFESSIONAL', 'COMMON'].includes(value),
  },
})

const aiAvatar = computed(() => props.avatar || APP_AVATARS[props.accent] || APP_AVATARS.agent)
const userAvatar = computed(() =>
  props.accent === 'love' ? APP_AVATARS.loveUser : APP_AVATARS.agentUser,
)

const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const messagesRef = ref(null)
const inputRef = ref(null)
const editInputRef = ref(null)
const copiedIndex = ref(-1)
const editingIndex = ref(-1)
const editText = ref('')
let abortController = null
let lastUserMessage = ''
let stopping = false
let copiedTimer = null

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(messages, scrollToBottom, { deep: true })

async function copyUserMessage(content, index) {
  const text = (content || '').trim()
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
  copiedIndex.value = index
  if (copiedTimer) clearTimeout(copiedTimer)
  copiedTimer = setTimeout(() => {
    copiedIndex.value = -1
  }, 1500)
}

function editUserMessage(index) {
  if (isLoading.value) return
  const msg = messages.value[index]
  if (!msg || msg.role !== 'user') return

  editingIndex.value = index
  editText.value = msg.content || ''
  nextTick(() => {
    editInputRef.value?.focus()
    editInputRef.value?.select()
    editInputRef.value?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  })
}

function closeEditModal() {
  editingIndex.value = -1
  editText.value = ''
}

function confirmEditAndSend() {
  const text = editText.value.trim()
  if (!text || isLoading.value) return
  closeEditModal()
  sendMessage(text)
}

function appendStepBubble(content) {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'ai' && last.loading) {
    last.content = content
    last.loading = false
    return
  }
  messages.value.push({ role: 'ai', content, loading: false })
}

function markStopped() {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'ai') {
    if (last.loading && !last.content) {
      last.content = '已停止生成'
    } else if (last.content && !last.content.includes('已停止生成')) {
      last.content += '\n\n[已停止生成]'
    }
    last.loading = false
  }
}

function finishLoadingState() {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'ai' && last.loading) {
    if (!last.content) {
      messages.value.pop()
    } else {
      last.loading = false
    }
  }
  isLoading.value = false
  abortController = null
  stopping = false
}

function getLastUserQuestion() {
  if (lastUserMessage.trim()) return lastUserMessage.trim()
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user' && messages.value[i].content?.trim()) {
      return messages.value[i].content.trim()
    }
  }
  return ''
}

function notifyStopApi(userQuestion) {
  const question = (userQuestion || '').trim()
  if (!question) {
    console.warn('[stop] 用户问题为空，跳过停止接口调用')
    return
  }

  // 停止接口固定为 stopChatByZhizhiManus，不会走 doChatBySynSSE / doChatByZhizhiManus
  const stopUrl =
    props.stopApiUrl || '/api/zhizhi-ai/stopChatByZhizhiManus'
  const params = new URLSearchParams({
    message: question,
    chatId: props.chatId || '',
    type: props.stopType || 'COMMON',
  })
  const url = `${stopUrl}?${params.toString()}`

  return fetch(url, {
    method: 'GET',
    cache: 'no-store',
    keepalive: true,
  }).catch((err) => {
    console.warn('[stop] 调用停止接口失败', err)
  })
}

function stopMessage() {
  if (!isLoading.value || stopping) return
  stopping = true

  const userQuestion = getLastUserQuestion()

  // 1. 先调用停止接口（与对话 SSE 接口完全分开）
  notifyStopApi(userQuestion)

  // 2. 再中断当前对话流（Network 里原 doChat* 请求会显示为 canceled，这是正常现象）
  if (abortController) {
    abortController.abort()
  }

  markStopped()
  isLoading.value = false
  abortController = null
  stopping = false
}

async function sendMessage(overrideText) {
  const text = (typeof overrideText === 'string' ? overrideText : inputText.value).trim()
  if (!text || isLoading.value) return

  lastUserMessage = text
  messages.value.push({ role: 'user', content: text })
  if (typeof overrideText !== 'string') {
    inputText.value = ''
  }

  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '', loading: true })
  isLoading.value = true
  stopping = false

  abortController = new AbortController()

  const params = { message: text }
  if (props.chatId) {
    params.chatId = props.chatId
  }

  try {
    await fetchSSE(
      props.apiUrl,
      params,
      (chunk) => {
        if (props.stepMode) {
          const content = chunk.trim()
          if (content) {
            appendStepBubble(content)
          }
        } else {
          messages.value[aiIndex].content += chunk
        }
      },
      abortController.signal,
    )
  } catch (err) {
    if (err.name !== 'AbortError') {
      const errorText = `[错误: ${err.message}]`
      if (props.stepMode) {
        appendStepBubble(errorText)
      } else {
        messages.value[aiIndex].content += `\n${errorText}`
      }
    }
  } finally {
    if (!stopping) {
      finishLoadingState()
    }
  }
}
</script>

<style scoped>
.chat-room-wrapper {
  --accent: var(--color-primary);
  --accent-soft: var(--color-primary-soft);
  --accent-hover: var(--color-primary-hover);

  position: relative;
  isolation: isolate;
  overflow: hidden;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding:
    calc(24px + var(--safe-top))
    calc(16px + var(--safe-right))
    calc(12px + var(--safe-bottom))
    calc(16px + var(--safe-left));
}

.chat-room-wrapper[data-accent='love'] {
  --accent: var(--color-accent-love);
  --accent-soft: var(--color-accent-love-soft);
  --accent-hover: #a84a57;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(56px);
  pointer-events: none;
  z-index: 0;
  animation: float-orb 14s ease-in-out infinite;
}

.orb-a {
  width: min(340px, 55vw);
  height: min(340px, 55vw);
  top: -10%;
  left: -8%;
  background: color-mix(in srgb, var(--accent) 35%, transparent);
}

.orb-b {
  width: min(300px, 50vw);
  height: min(300px, 50vw);
  right: -10%;
  bottom: 5%;
  background: rgba(95, 168, 192, 0.2);
  animation-delay: -5s;
}

.chat-room {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 720px;
  height: calc(100dvh - 88px - var(--safe-top) - var(--safe-bottom));
  max-height: 780px;
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.68));
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card), 0 0 0 1px rgba(31, 111, 139, 0.04);
  overflow: hidden;
  animation: page-enter 0.45s ease-out;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.55);
  border-bottom: 1px solid rgba(15, 28, 46, 0.06);
  flex-shrink: 0;
}

:deep(.site-footer) {
  position: relative;
  z-index: 1;
}

.back-btn {
  flex-shrink: 0;
  min-height: 36px;
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface-soft);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition:
    color 0.2s,
    border-color 0.2s,
    background 0.2s;
}

@media (hover: hover) and (pointer: fine) {
  .back-btn:hover {
    color: var(--accent);
    border-color: var(--accent);
    background: var(--accent-soft);
  }

  .send-btn:hover:not(:disabled) {
    background: var(--accent-hover);
  }
}

.header-info {
  min-width: 0;
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.app-mark {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: var(--shadow-soft);
}

.header-info h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(15px, 2.4vw, 17px);
  font-weight: 600;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-id {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: 16px 14px;
  background:
    radial-gradient(ellipse 80% 40% at 50% 0%, color-mix(in srgb, var(--accent) 8%, transparent), transparent 70%),
    linear-gradient(180deg, rgba(244, 248, 252, 0.72), rgba(240, 245, 249, 0.85));
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  height: 100%;
  color: var(--color-text-muted);
  font-size: clamp(13px, 2.2vw, 14px);
  text-align: center;
  padding: 0 20px;
  line-height: 1.7;
}

.empty-avatar {
  width: 68px;
  height: 68px;
  border-radius: 20px;
  object-fit: cover;
  box-shadow: 0 10px 28px color-mix(in srgb, var(--accent) 28%, transparent);
  animation: pulse-soft 3.2s ease-in-out infinite;
}

.empty-tip p {
  margin: 0;
}

.message-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 14px;
  animation: page-enter 0.28s ease-out;
}

.message-row-user {
  align-items: flex-end;
}

.message-row-ai {
  align-items: flex-start;
}

.message-item {
  display: flex;
  gap: 8px;
  max-width: 100%;
}

.message-user {
  flex-direction: row-reverse;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
  margin-right: 42px;
  opacity: 0.55;
  transition: opacity 0.2s;
}

.message-row-user:hover .user-actions {
  opacity: 1;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.2s,
    color 0.2s;
}

.action-btn svg {
  width: 15px;
  height: 15px;
}

.action-btn:hover:not(:disabled) {
  background: rgba(15, 28, 46, 0.06);
  color: var(--accent);
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.copied-tip {
  margin-left: 4px;
  font-size: 11px;
  color: var(--accent);
}

.avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  object-fit: cover;
  box-shadow: var(--shadow-soft);
}

.bubble {
  max-width: min(75%, 520px);
  padding: 10px 13px;
  border-radius: 14px;
  line-height: 1.55;
  font-size: clamp(13px, 2vw, 14px);
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.message-ai .bubble {
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-text);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-top-left-radius: 4px;
  box-shadow: var(--shadow-soft);
}

.message-user .bubble {
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent) 78%, #000));
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 6px 16px color-mix(in srgb, var(--accent) 35%, transparent);
}

.bubble-text {
  margin: 0;
}

.typing-cursor {
  animation: blink 1s step-end infinite;
  color: var(--accent);
  font-weight: bold;
}

.composer {
  flex-shrink: 0;
  padding: 10px 14px 8px;
  background: rgba(255, 255, 255, 0.62);
  border-top: 1px solid rgba(15, 28, 46, 0.06);
}

.composer-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 12px 10px;
  border: 1px solid rgba(15, 28, 46, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 18px rgba(7, 21, 37, 0.06);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.composer-box:focus-within {
  border-color: color-mix(in srgb, var(--accent) 45%, transparent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.chat-input {
  width: 100%;
  min-height: 48px;
  max-height: 140px;
  padding: 2px 4px;
  border: none;
  border-radius: 0;
  /* 16px 避免 iOS 聚焦时自动放大 */
  font-size: 16px;
  resize: none;
  outline: none;
  line-height: 1.55;
  color: var(--color-text);
  background: transparent;
}

.chat-input:disabled {
  opacity: 0.7;
}

.send-btn,
.stop-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent) 78%, #000));
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 14px color-mix(in srgb, var(--accent) 28%, transparent);
  transition:
    transform 0.15s,
    box-shadow 0.2s,
    background 0.2s,
    opacity 0.2s;
}

.send-btn:hover:not(:disabled),
.stop-btn:hover {
  background: var(--accent-hover);
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent) 38%, transparent);
}

.send-btn:active:not(:disabled),
.stop-btn:active {
  transform: scale(0.96);
}

.send-btn:disabled {
  background: #c5d0da;
  box-shadow: none;
  cursor: not-allowed;
  opacity: 0.7;
}

.send-icon {
  width: 18px;
  height: 18px;
}

.stop-square {
  width: 11px;
  height: 11px;
  border-radius: 2.5px;
  background: #fff;
}

.ai-disclaimer {
  margin: 8px 0 2px;
  text-align: center;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-muted);
}

.inline-edit {
  align-self: center;
  width: min(88%, 520px);
  margin: 8px 0 4px;
  padding: 12px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(15, 28, 46, 0.1);
  box-shadow: 0 4px 16px rgba(7, 21, 37, 0.06);
  animation: page-enter 0.22s ease-out;
}

.edit-textarea {
  width: 100%;
  min-height: 72px;
  padding: 4px 2px 8px;
  border: none;
  border-radius: 0;
  resize: none;
  outline: none;
  font-size: 14px;
  line-height: 1.55;
  color: var(--color-text);
  background: transparent;
  font-family: inherit;
}

.edit-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}

.edit-cancel,
.edit-confirm {
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.edit-cancel {
  border: 1px solid rgba(15, 28, 46, 0.12);
  background: #fff;
  color: var(--color-text);
}

.edit-cancel:hover {
  background: rgba(15, 28, 46, 0.04);
}

.edit-confirm {
  border: none;
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent) 78%, #000));
  color: #fff;
}

.edit-confirm:hover:not(:disabled) {
  background: var(--accent-hover);
}

.edit-confirm:disabled {
  background: #c5d0da;
  cursor: not-allowed;
}

/* 平板 */
@media (max-width: 1024px) {
  .chat-room {
    max-width: 680px;
    max-height: none;
    height: calc(100dvh - 80px - var(--safe-top) - var(--safe-bottom));
  }

  .chat-room-wrapper {
    padding:
      calc(20px + var(--safe-top))
      calc(16px + var(--safe-right))
      calc(10px + var(--safe-bottom))
      calc(16px + var(--safe-left));
  }
}

/* 手机：全屏沉浸 */
@media (max-width: 768px) {
  .chat-room-wrapper {
    align-items: stretch;
    justify-content: flex-start;
    gap: 0;
    padding: 0;
    padding-top: var(--safe-top);
    padding-bottom: var(--safe-bottom);
  }

  .chat-room {
    flex: 1;
    max-width: none;
    height: auto;
    max-height: none;
    border-radius: 0;
    border: none;
    box-shadow: none;
  }

  .chat-header {
    padding: 10px 12px;
  }

  .chat-messages {
    padding: 14px 12px;
  }

  .bubble {
    max-width: 82%;
  }

  .empty-tip {
    padding: 0 16px;
  }

  .composer {
    padding: 10px 12px 6px;
  }

  .composer-box {
    border-radius: 16px;
    padding: 10px 10px 8px;
  }

  .user-actions {
    opacity: 0.85;
    margin-right: 38px;
  }
}

/* 小屏手机 */
@media (max-width: 480px) {
  .back-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .app-mark {
    width: 26px;
    height: 26px;
    border-radius: 8px;
  }

  .empty-avatar {
    width: 56px;
    height: 56px;
  }

  .avatar {
    width: 30px;
    height: 30px;
    border-radius: 8px;
  }

  .bubble {
    max-width: 86%;
    padding: 9px 11px;
  }

  .send-btn,
  .stop-btn {
    width: 34px;
    height: 34px;
  }

  .ai-disclaimer {
    font-size: 11px;
  }
}

/* 桌面大屏 */
@media (min-width: 1280px) {
  .chat-room {
    max-width: 760px;
  }
}

/* 横屏矮屏 */
@media (max-height: 560px) and (orientation: landscape) {
  .chat-room-wrapper {
    padding:
      calc(8px + var(--safe-top))
      calc(12px + var(--safe-right))
      calc(4px + var(--safe-bottom))
      calc(12px + var(--safe-left));
    gap: 4px;
  }

  .chat-room {
    height: calc(100dvh - 48px - var(--safe-top) - var(--safe-bottom));
    max-height: none;
  }

  .chat-header {
    padding: 8px 12px;
  }

  .chat-id {
    display: none;
  }
}
</style>
