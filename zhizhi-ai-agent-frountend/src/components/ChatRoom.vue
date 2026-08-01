<template>
  <div class="chat-room-wrapper" :data-accent="accent">
    <ParticleBackground density="light" />
    <div class="orb orb-a" aria-hidden="true" />
    <div class="orb orb-b" aria-hidden="true" />

    <div class="chat-layout">
      <aside class="history-sidebar" :class="{ open: sidebarOpen }">
        <div class="sidebar-top">
          <span class="sidebar-title">历史会话</span>
          <button class="new-chat-btn" type="button" @click="startNewChat">
            新对话
          </button>
        </div>
        <p v-if="historyError" class="sidebar-hint sidebar-error">{{ historyError }}</p>
        <p v-else-if="historyLoading" class="sidebar-hint">加载中…</p>
        <p v-else-if="conversations.length === 0" class="sidebar-hint">暂无历史，发一条消息开始吧</p>
        <ul v-else class="conversation-list">
          <li
            v-for="item in conversations"
            :key="item.chatId"
            :class="['conversation-item', { active: item.chatId === chatId }]"
          >
            <button
              type="button"
              class="conversation-main"
              :disabled="isLoading"
              @click="selectConversation(item.chatId)"
            >
              <span class="conversation-name">{{ item.title || '未命名会话' }}</span>
              <span class="conversation-time">{{ formatTime(item.updateDate) }}</span>
            </button>
            <button
              type="button"
              class="conversation-delete"
              title="删除"
              :disabled="isLoading"
              @click.stop="removeConversation(item.chatId)"
            >
              ×
            </button>
          </li>
        </ul>
      </aside>

      <button
        v-if="sidebarOpen"
        class="sidebar-backdrop"
        type="button"
        aria-label="关闭历史"
        @click="sidebarOpen = false"
      />

      <div class="chat-room">
      <header class="chat-header">
        <button class="back-btn" type="button" @click="$router.push('/')">返回</button>
        <button
          class="history-toggle"
          type="button"
          :aria-pressed="sidebarOpen"
          @click="sidebarOpen = !sidebarOpen"
        >
          历史
        </button>
        <div class="header-info">
          <div class="title-row">
            <img class="app-mark" :src="aiAvatar" :alt="`${title}头像`" />
            <h1>{{ title }}</h1>
          </div>
          <span v-if="chatId" class="chat-id">会话 {{ shortId(chatId) }}</span>
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
            <!-- 用户气泡：保持原样式 -->
            <div v-if="msg.role === 'user'" class="bubble">
              <p class="bubble-text">{{ msg.content }}</p>
            </div>

            <!-- AI 气泡：超级智能体思考区 + 回答 -->
            <div
              v-else
              class="bubble"
              :class="{ 'bubble-agent': !!msg.thinking }"
            >
              <div
                v-if="msg.thinking && msg.thinking.status !== 'idle'"
                class="thinking-panel"
              >
                <button
                  type="button"
                  class="thinking-header"
                  @click="toggleThinking(msg)"
                >
                  <span class="thinking-left">
                    <span class="thinking-icon" aria-hidden="true">◎</span>
                    <span class="thinking-title">
                      {{ thinkingTitle(msg) }}
                    </span>
                  </span>
                  <span class="thinking-chevron" aria-hidden="true">
                    {{ msg.thinking.collapsed ? '▾' : '▴' }}
                  </span>
                </button>
                <div v-show="!msg.thinking.collapsed" class="thinking-body">
                  <pre class="thinking-text">{{ msg.thinking.text || ' ' }}</pre>
                </div>
              </div>

              <div
                v-if="msg.answer?.text || msg.answer?.status === 'streaming'"
                class="answer-body markdown-body"
                v-html="renderMarkdown(msg.answer.text)"
              />
              <template v-else-if="!msg.thinking">
                <p class="bubble-text">{{ msg.content }}</p>
              </template>
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
          <div class="composer-toolbar">
            <label class="model-select-wrap" title="选择大模型">
              <span class="model-select-icon" aria-hidden="true">✦</span>
              <select
                v-model="selectedModel"
                class="model-select"
                :disabled="isLoading"
                aria-label="选择大模型"
              >
                <option v-for="m in modelOptions" :key="m.id" :value="m.id">
                  {{ m.label }}
                </option>
              </select>
              <span class="model-select-chevron" aria-hidden="true">▾</span>
            </label>
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
        </div>
        <p class="ai-disclaimer">内容由AI生成，请仔细斟酌</p>
      </div>
    </div>
    </div>
    <SiteFooter compact />
  </div>
</template>

<script setup>
import { computed, ref, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { fetchSSE } from '../api/sse.js'
import { resolveApiUrl } from '../api/config.js'
import {
  addMessage,
  deleteConversation,
  ensureConversation,
  listConversations,
  listMessages,
  updateConversation,
} from '../api/conversation.js'
import { APP_AVATARS } from '../constants/apps.js'
import { DEFAULT_MODEL, MODEL_OPTIONS } from '../constants/models.js'
import { generateChatId } from '../utils/chatId.js'
import { authHeader } from '../utils/auth.js'
import SiteFooter from './SiteFooter.vue'
import ParticleBackground from './ParticleBackground.vue'

marked.setOptions({
  gfm: true,
  breaks: false,
})

/** 压缩多余空行，避免结论区段落/列表间距过大 */
function compactMarkdownSource(text) {
  return String(text)
    .replace(/\r\n/g, '\n')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    // 标题 / 加粗行后的空行
    .replace(/(\*\*[^*\n]+\*\*)\n\n+/g, '$1\n')
    .replace(/^(#{1,6}\s+[^\n]+)\n\n+/gm, '$1\n')
    // 列表项之间、分类标题与列表之间去掉空行
    .replace(/\n\n+(?=(\s*[-*+•○] |\s*\d+\. ))/g, '\n')
    .replace(/([^\n])\n\n+(?=\d+\. )/g, '$1\n')
    .trim()
}

function renderMarkdown(text) {
  if (!text) return ''
  let html = marked.parse(compactMarkdownSource(text), { async: false })
  // 去掉空段落、多余换行标签造成的视觉空隙
  html = String(html)
    .replace(/<p>\s*(?:<br\s*\/?>)?\s*<\/p>/gi, '')
    .replace(/(<\/(?:li|p|h[1-6])>)\s+(?=<(?:li|p|h[1-6]|ul|ol))/gi, '$1')
  return DOMPurify.sanitize(html)
}

function thinkingTitle(msg) {
  if (!msg?.thinking) return ''
  if (msg.thinking.status === 'in_progress') return '思考中...'
  const seconds = Math.max(1, Math.round((msg.thinking.elapsedMs || 0) / 1000))
  return `思考完成（用时 ${seconds} 秒）`
}

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  apiUrl: {
    type: String,
    required: true,
  },
  /** 可选初始 chatId；不传则组件内生成 32 位 ID */
  initialChatId: {
    type: String,
    default: '',
  },
  /** LOVE_MASTER | SUPER_AGENT，用于历史侧栏过滤与建会话 */
  agentType: {
    type: String,
    required: true,
  },
  /** 是否启用历史侧栏与消息落库（需 MYSQL_ENABLED=true） */
  enableHistory: {
    type: Boolean,
    default: true,
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
  /** 停止接口 type：AI面试官小助手CC PROFESSIONAL，超级智能体 COMMON */
  stopType: {
    type: String,
    default: 'COMMON',
    validator: (value) => ['PROFESSIONAL', 'COMMON'].includes(value),
  },
  /** 默认大模型：deepseek | qwen | doubao */
  defaultModel: {
    type: String,
    default: DEFAULT_MODEL,
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
const modelOptions = MODEL_OPTIONS
const selectedModel = ref(props.defaultModel || DEFAULT_MODEL)
const chatId = ref(props.initialChatId || generateChatId())
const conversations = ref([])
const historyLoading = ref(false)
const historyError = ref('')
const sidebarOpen = ref(typeof window !== 'undefined' ? window.innerWidth >= 960 : true)
const conversationReady = ref(false)
let abortController = null
let lastUserMessage = ''
let stopping = false
let copiedTimer = null

function shortId(id) {
  if (!id) return ''
  return id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

function titleFromText(text) {
  const t = (text || '').trim().replace(/\s+/g, ' ')
  if (!t) return '新对话'
  return t.length > 24 ? `${t.slice(0, 24)}…` : t
}

async function refreshConversations() {
  if (!props.enableHistory) return
  historyLoading.value = true
  historyError.value = ''
  try {
    const list = await listConversations({ agentType: props.agentType })
    conversations.value = Array.isArray(list) ? list : []
  } catch (err) {
    historyError.value = err.message || '历史加载失败（请确认 MySQL 已启用）'
    conversations.value = []
  } finally {
    historyLoading.value = false
  }
}

function mapStoredMessages(rows) {
  return (rows || []).map((row) => {
    if (row.role === 'user') {
      return { role: 'user', content: row.content || '' }
    }
    // assistant / system / tool -> AI 气泡
    if (props.stepMode) {
      const msg = createAgentMessage()
      msg.loading = false
      msg.thinking.status = 'done'
      msg.thinking.collapsed = true
      msg.thinking.text = ''
      msg.answer.status = 'done'
      msg.answer.text = row.content || ''
      msg.answer.fullText = row.content || ''
      msg.content = row.content || ''
      return msg
    }
    return { role: 'ai', content: row.content || '', loading: false }
  })
}

async function selectConversation(nextChatId) {
  if (!nextChatId || nextChatId === chatId.value || isLoading.value) return
  if (abortController) abortController.abort()
  resetTypewriterState()
  chatId.value = nextChatId
  conversationReady.value = true
  messages.value = []
  try {
    const rows = await listMessages(nextChatId)
    messages.value = mapStoredMessages(rows)
  } catch (err) {
    historyError.value = err.message || '加载消息失败'
  }
  if (typeof window !== 'undefined' && window.innerWidth < 960) {
    sidebarOpen.value = false
  }
}

async function startNewChat() {
  // 允许随时开新会话：打断进行中的生成，避免按钮因 isLoading 一直无响应
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  stopping = false
  isLoading.value = false
  resetTypewriterState()

  const current = conversations.value.find((c) => c.chatId === chatId.value)
  const alreadyFresh =
    messages.value.length === 0 &&
    (!current || !current.title || current.title === '新对话')

  // 已在空白「新对话」上：只聚焦输入框，避免连点刷出一堆空会话
  if (alreadyFresh && current) {
    await nextTick()
    inputRef.value?.focus()
    if (typeof window !== 'undefined' && window.innerWidth < 960) {
      sidebarOpen.value = false
    }
    return
  }

  chatId.value = generateChatId()
  conversationReady.value = false
  messages.value = []
  inputText.value = ''

  if (props.enableHistory) {
    try {
      await ensureConversation({
        chatId: chatId.value,
        agentType: props.agentType,
        title: '新对话',
        model: selectedModel.value,
      })
      conversationReady.value = true
      await refreshConversations()
    } catch (err) {
      historyError.value = err.message || '创建会话失败'
    }
  }

  await nextTick()
  inputRef.value?.focus()
  if (typeof window !== 'undefined' && window.innerWidth < 960) {
    sidebarOpen.value = false
  }
}

async function removeConversation(targetChatId) {
  if (!targetChatId || isLoading.value) return
  try {
    await deleteConversation(targetChatId)
    if (chatId.value === targetChatId) {
      await startNewChat()
    }
    await refreshConversations()
  } catch (err) {
    historyError.value = err.message || '删除失败'
  }
}

async function persistUserAndEnsure(text) {
  if (!props.enableHistory) return
  try {
    if (!conversationReady.value) {
      await ensureConversation({
        chatId: chatId.value,
        agentType: props.agentType,
        title: titleFromText(text),
        model: selectedModel.value,
      })
      conversationReady.value = true
    }
    await addMessage(chatId.value, { role: 'user', content: text })
    await refreshConversations()
  } catch (err) {
    console.warn('[history] 保存用户消息失败', err)
    historyError.value = err.message || '保存失败'
  }
}

function extractAiPersistText(aiMsg) {
  if (!aiMsg) return ''
  if (aiMsg.answer?.fullText) return aiMsg.answer.fullText
  if (aiMsg.answer?.text) return aiMsg.answer.text
  return aiMsg.content || ''
}

async function persistAssistantReply(aiMsg) {
  if (!props.enableHistory || !conversationReady.value) return
  const content = extractAiPersistText(aiMsg).trim()
  if (!content) return
  try {
    const metadata = aiMsg?.thinking?.text
      ? JSON.stringify({ thinking: aiMsg.thinking.text })
      : undefined
    await addMessage(chatId.value, {
      role: 'assistant',
      content,
      metadata,
    })
    // 用首条用户问题刷新标题（若仍是默认）
    const current = conversations.value.find((c) => c.chatId === chatId.value)
    if (current && (!current.title || current.title === '新对话')) {
      const firstUser = messages.value.find((m) => m.role === 'user')
      if (firstUser?.content) {
        await updateConversation(chatId.value, { title: titleFromText(firstUser.content) })
      }
    }
    await refreshConversations()
  } catch (err) {
    console.warn('[history] 保存助手消息失败', err)
  }
}

onMounted(() => {
  refreshConversations()
})

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

function createAgentMessage() {
  return {
    role: 'ai',
    content: '',
    loading: true,
    thinking: {
      status: 'in_progress',
      text: '',
      collapsed: false,
      startedAt: Date.now(),
      elapsedMs: 0,
    },
    answer: {
      status: 'idle',
      text: '',
      fullText: '',
    },
  }
}

/** 打字机：思考区与结论区共用，逐字吐出，无闪烁光标 */
const TYPEWRITER_DELAY_MS = 48
const TYPEWRITER_CHARS_PER_TICK = 1

let typewriterTimer = null
let typewriterGen = 0
/** @type {string[]} */
let pendingThinkingChars = []
/** @type {object | null} */
let typewriterMsg = null
let thinkingFinishRequested = false
/** @type {string | null} */
let pendingAnswerFullText = null

function clearTypewriterTimer() {
  if (typewriterTimer != null) {
    clearTimeout(typewriterTimer)
    typewriterTimer = null
  }
}

function resetTypewriterState() {
  clearTypewriterTimer()
  typewriterGen += 1
  pendingThinkingChars = []
  typewriterMsg = null
  thinkingFinishRequested = false
  pendingAnswerFullText = null
}

function hasThinkingPending() {
  return pendingThinkingChars.length > 0
}

function enqueueThinkingText(msg, chunk, separator = '\n\n') {
  if (!msg?.thinking || !chunk) return
  typewriterMsg = msg
  const needSep = Boolean(msg.thinking.text) || pendingThinkingChars.length > 0
  const piece = needSep ? `${separator}${chunk}` : chunk
  pendingThinkingChars.push(...Array.from(piece))
  ensureTypewriterPump()
}

function applyThinkingFinished(msg) {
  if (!msg?.thinking) return
  msg.thinking.status = 'done'
  msg.thinking.collapsed = true
  thinkingFinishRequested = false
}

function beginAnswerTypewriter(msg, fullText) {
  const text = String(fullText || '')
  const chars = Array.from(text)
  msg.answer.fullText = text
  msg.answer.status = 'streaming'
  msg.answer.text = ''
  msg.content = ''
  msg.loading = true
  pendingAnswerFullText = null

  if (!chars.length) {
    msg.answer.status = 'done'
    msg.loading = false
    isLoading.value = false
    persistAssistantReply(msg)
    return
  }

  // 复用 pendingThinkingChars 槽位之外：把结论字符放进 answer 专用队列
  msg.answer._pendingChars = chars
  msg.answer._revealIndex = 0
  ensureTypewriterPump()
}

function startAnswerTypewriter(msg, fullText) {
  // 若思考还在打字，等排空后再开始结论
  if (hasThinkingPending() || thinkingFinishRequested) {
    pendingAnswerFullText = String(fullText || '')
    typewriterMsg = msg
    ensureTypewriterPump()
    return
  }
  beginAnswerTypewriter(msg, fullText)
}

function stopAnswerTypewriter(msg, { flush = false } = {}) {
  clearTypewriterTimer()
  typewriterGen += 1
  if (pendingThinkingChars.length && (msg || typewriterMsg)?.thinking) {
    const target = msg || typewriterMsg
    target.thinking.text =
      (target.thinking.text || '') + pendingThinkingChars.join('')
  }
  pendingThinkingChars = []
  thinkingFinishRequested = false
  pendingAnswerFullText = null

  if (!msg?.answer) {
    typewriterMsg = null
    return
  }
  if (Array.isArray(msg.answer._pendingChars)) {
    if (flush || msg.answer.fullText) {
      msg.answer.text = msg.answer.fullText || msg.answer._pendingChars.join('')
      msg.content = msg.answer.text
    }
    delete msg.answer._pendingChars
    delete msg.answer._revealIndex
  } else if (flush && msg.answer.fullText) {
    msg.answer.text = msg.answer.fullText
    msg.content = msg.answer.text
  }
  if (msg.answer.status === 'streaming') {
    msg.answer.status = 'done'
  }
  msg.loading = false
  typewriterMsg = null
}

function ensureTypewriterPump() {
  if (typewriterTimer != null) return
  const gen = typewriterGen
  const msg = typewriterMsg
  if (!msg) return

  const tick = () => {
    if (gen !== typewriterGen) return

    // 1) 先吐思考区
    if (pendingThinkingChars.length > 0) {
      const n = Math.min(TYPEWRITER_CHARS_PER_TICK, pendingThinkingChars.length)
      const next = pendingThinkingChars.splice(0, n).join('')
      msg.thinking.text = (msg.thinking.text || '') + next
      msg.thinking.status = 'in_progress'
      msg.thinking.collapsed = false
      typewriterTimer = setTimeout(tick, TYPEWRITER_DELAY_MS)
      return
    }

    // 2) 思考排空后，落实 thinking_done
    if (thinkingFinishRequested) {
      applyThinkingFinished(msg)
    }

    // 3) 有排队的最终结论，开始打字
    if (pendingAnswerFullText != null && msg.answer?.status !== 'streaming') {
      const full = pendingAnswerFullText
      pendingAnswerFullText = null
      beginAnswerTypewriter(msg, full)
      // beginAnswerTypewriter 会再次 ensure；此处继续走结论分支
    }

    // 4) 吐结论区
    const pending = msg.answer?._pendingChars
    if (Array.isArray(pending) && msg.answer.status === 'streaming') {
      let index = msg.answer._revealIndex || 0
      if (index < pending.length) {
        const n = Math.min(TYPEWRITER_CHARS_PER_TICK, pending.length - index)
        index += n
        msg.answer._revealIndex = index
        msg.answer.text = pending.slice(0, index).join('')
        msg.content = msg.answer.text
        typewriterTimer = setTimeout(tick, TYPEWRITER_DELAY_MS)
        return
      }
      delete msg.answer._pendingChars
      delete msg.answer._revealIndex
      msg.answer.status = 'done'
      msg.loading = false
      isLoading.value = false
      typewriterTimer = null
      typewriterMsg = null
      persistAssistantReply(msg)
      return
    }

    typewriterTimer = null
    if (!hasThinkingPending() && msg.answer?.status !== 'streaming') {
      typewriterMsg = null
    }
  }

  typewriterTimer = setTimeout(tick, 0)
}

function requestThinkingFinish(msg) {
  typewriterMsg = msg
  thinkingFinishRequested = true
  if (!hasThinkingPending()) {
    applyThinkingFinished(msg)
    if (pendingAnswerFullText != null) {
      ensureTypewriterPump()
    }
  } else {
    ensureTypewriterPump()
  }
}

function ensureAgentMessage(aiIndex) {
  const current = messages.value[aiIndex]
  if (current?.role === 'ai' && current.thinking && current.answer) {
    return current
  }
  const agentMsg = createAgentMessage()
  if (current?.role === 'ai') {
    messages.value[aiIndex] = agentMsg
  }
  return messages.value[aiIndex]
}

function isAgentEventPayload(raw) {
  if (!raw || typeof raw !== 'string') return false
  const trimmed = raw.trim()
  if (!trimmed.startsWith('{')) return false
  try {
    const parsed = JSON.parse(trimmed)
    return Boolean(parsed && typeof parsed.type === 'string')
  } catch {
    return false
  }
}

/** 把思考区里嵌套的 JSON 尽量转成可读文本 */
function formatThinkingText(text) {
  if (!text) return ''
  const headerMatch = text.match(/^(【步骤\s*\d+\s*·\s*[^】]+】)\s*\n?([\s\S]*)$/)
  const header = headerMatch ? headerMatch[1] : ''
  const body = (headerMatch ? headerMatch[2] : text).trim()

  if (body.startsWith('{') || body.startsWith('[')) {
    try {
      const obj = JSON.parse(body)
      const lines = []
      if (header) lines.push(header)
      if (obj.summary) lines.push(String(obj.summary))
      if (Array.isArray(obj.steps)) {
        lines.push('')
        obj.steps.forEach((step, idx) => {
          const stepText =
            typeof step === 'string'
              ? step
              : step?.detail || step?.name || JSON.stringify(step)
          lines.push(`${idx + 1}. ${stepText}`)
        })
      }
      if (obj.metadata?.next_action_suggestion) {
        lines.push('')
        lines.push(`下一步建议：${obj.metadata.next_action_suggestion}`)
      }
      if (lines.length > (header ? 1 : 0)) {
        return lines.join('\n')
      }
    } catch {
      // ignore and fall through
    }
  }
  return text
}

function formatAnswerText(text) {
  if (!text) return ''
  const trimmed = text.trim()
  if (!(trimmed.startsWith('{') || trimmed.startsWith('['))) {
    return text
  }
  try {
    const obj = JSON.parse(trimmed)
    const lines = []
    if (obj.summary) lines.push(String(obj.summary))
    if (Array.isArray(obj.steps)) {
      lines.push('')
      obj.steps.forEach((step, idx) => {
        if (typeof step === 'string') {
          lines.push(`${idx + 1}. ${step}`)
        } else if (step && typeof step === 'object') {
          const title = step.name || `步骤 ${idx + 1}`
          const detail = step.detail || step.description || ''
          lines.push(`${idx + 1}. ${title}${detail ? `：${detail}` : ''}`)
        }
      })
    }
    if (obj.metadata?.next_action_suggestion) {
      lines.push('')
      lines.push(`建议：${obj.metadata.next_action_suggestion}`)
    }
    return lines.length ? lines.join('\n') : text
  } catch {
    return text
  }
}

function toggleThinking(msg) {
  if (!msg?.thinking) return
  // 思考中不允许折叠，方便看实时过程
  if (msg.thinking.status === 'in_progress') return
  msg.thinking.collapsed = !msg.thinking.collapsed
}

function handleAgentEvent(raw, aiIndex) {
  const msg = ensureAgentMessage(aiIndex)
  if (!msg) return

  let event = null
  try {
    event = JSON.parse(raw.trim())
    // 兼容被二次 JSON 序列化的情况：`"{\"type\":\"...\"}"`
    if (typeof event === 'string') {
      event = JSON.parse(event)
    }
  } catch {
    // 兼容旧的纯文本 step 输出
    msg.thinking.status = 'in_progress'
    msg.thinking.collapsed = false
    enqueueThinkingText(msg, raw)
    return
  }

  if (!event?.type) {
    msg.thinking.status = 'in_progress'
    msg.thinking.collapsed = false
    enqueueThinkingText(msg, raw)
    return
  }

  switch (event.type) {
    case 'thinking_start':
      msg.thinking.status = 'in_progress'
      msg.thinking.collapsed = false
      msg.thinking.text = msg.thinking.text || ''
      msg.thinking.startedAt = Date.now()
      msg.thinking.elapsedMs = 0
      msg.loading = true
      typewriterMsg = msg
      break
    case 'thinking_delta':
      msg.thinking.status = 'in_progress'
      msg.thinking.collapsed = false
      if (event.text) {
        const readable = formatThinkingText(event.text)
        // 过滤仍可能漏出的原始工具 dump
        if (/工具 .+ 完成了它的任务/.test(readable) || /\\"title\\"/.test(readable)) {
          break
        }
        enqueueThinkingText(msg, readable, '\n\n')
      }
      break
    case 'tool_done':
      msg.thinking.status = 'in_progress'
      msg.thinking.collapsed = false
      if (event.text) {
        enqueueThinkingText(msg, `✓ ${event.text}`, '\n')
      }
      break
    case 'thinking_done':
      if (typeof event.elapsedMs === 'number') {
        msg.thinking.elapsedMs = event.elapsedMs
      } else if (msg.thinking.startedAt) {
        msg.thinking.elapsedMs = Date.now() - msg.thinking.startedAt
      }
      if (event.text && !msg.thinking.text && !hasThinkingPending()) {
        enqueueThinkingText(
          msg,
          String(event.text)
            .split(/\n\n+/)
            .map((part) => formatThinkingText(part))
            .join('\n\n'),
          '',
        )
      }
      requestThinkingFinish(msg)
      break
    case 'answer_done':
      startAnswerTypewriter(msg, formatAnswerText(event.text || ''))
      break
    case 'error':
      stopAnswerTypewriter(msg, { flush: true })
      msg.answer.status = 'done'
      msg.answer.text = event.text || '发生错误'
      msg.answer.fullText = msg.answer.text
      msg.content = msg.answer.text
      if (msg.thinking.status === 'in_progress' || thinkingFinishRequested) {
        applyThinkingFinished(msg)
      }
      msg.loading = false
      break
    default:
      break
  }
}

function markStopped() {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'ai') {
    stopAnswerTypewriter(last, { flush: true })
    if (last.thinking?.status === 'in_progress' || thinkingFinishRequested) {
      applyThinkingFinished(last)
    }
    if (!last.thinking?.text && last.thinking) {
      last.thinking.text = '已停止生成'
    }
    if (!last.content && !last.answer?.text) {
      last.content = '已停止生成'
      last.answer = { status: 'done', text: '已停止生成', fullText: '已停止生成' }
    } else if (last.answer?.text && !last.answer.text.includes('已停止生成')) {
      last.answer.text += '\n\n[已停止生成]'
      last.content = last.answer.text
    } else if (last.content && !last.content.includes('已停止生成')) {
      last.content += '\n\n[已停止生成]'
    }
    last.loading = false
    isLoading.value = false
  }
}

function finishLoadingState() {
  const last = messages.value[messages.value.length - 1]
  // SSE 已结束但仍在打字（思考或结论）：保持 loading，等打字机收尾
  if (
    last?.role === 'ai' &&
    (last.answer?.status === 'streaming' ||
      hasThinkingPending() ||
      pendingAnswerFullText != null ||
      thinkingFinishRequested)
  ) {
    abortController = null
    stopping = false
    return
  }
  if (last?.role === 'ai' && last.loading) {
    const hasContent =
      last.content || last.answer?.text || last.thinking?.text
    if (!hasContent) {
      messages.value.pop()
    } else {
      last.loading = false
      if (last.thinking?.status === 'in_progress') {
        applyThinkingFinished(last)
      }
    }
  }
  const lastAi = messages.value[messages.value.length - 1]
  if (lastAi?.role === 'ai') {
    persistAssistantReply(lastAi)
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
  const stopUrl = resolveApiUrl(
    props.stopApiUrl || '/api/zhizhi-ai/stopChatByZhizhiManus',
  )
  const params = new URLSearchParams({
    message: question,
    chatId: chatId.value || '',
    type: props.stopType || 'COMMON',
  })
  const url = `${stopUrl}?${params.toString()}`

  return fetch(url, {
    method: 'GET',
    cache: 'no-store',
    keepalive: true,
    headers: {
      ...authHeader(),
    },
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

  resetTypewriterState()
  lastUserMessage = text
  messages.value.push({ role: 'user', content: text })
  if (typeof overrideText !== 'string') {
    inputText.value = ''
  }

  await persistUserAndEnsure(text)

  const aiIndex = messages.value.length
  const useAgentUi =
    props.stepMode || String(props.apiUrl || '').includes('ZhizhiManus')
  messages.value.push(
    useAgentUi ? createAgentMessage() : { role: 'ai', content: '', loading: true },
  )
  isLoading.value = true
  stopping = false

  abortController = new AbortController()

  const params = {
    message: text,
    model: selectedModel.value || DEFAULT_MODEL,
  }
  if (chatId.value) {
    params.chatId = chatId.value
  }

  try {
    await fetchSSE(
      props.apiUrl,
      params,
      (chunk) => {
        const content = (chunk || '').trim()
        if (!content) return

        // 智能体结构化事件：始终走思考/回答面板，避免把 JSON 当普通气泡
        if (useAgentUi || isAgentEventPayload(content)) {
          handleAgentEvent(content, aiIndex)
          return
        }
        messages.value[aiIndex].content += chunk
      },
      abortController.signal,
    )
  } catch (err) {
    if (err.name !== 'AbortError') {
      const errorText = `[错误: ${err.message}]`
      if (useAgentUi) {
        handleAgentEvent(
          JSON.stringify({ type: 'error', text: errorText }),
          aiIndex,
        )
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

onUnmounted(() => {
  resetTypewriterState()
})
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
  --accent-hover: #246355;
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

.bubble-agent {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: min(88%, 640px);
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0;
}

.thinking-panel {
  border: 1px solid rgba(15, 28, 46, 0.1);
  border-radius: 14px;
  background: rgba(244, 247, 251, 0.95);
  overflow: hidden;
}

.thinking-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--color-text);
  font: inherit;
}

.thinking-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.thinking-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--accent);
  background: var(--accent-soft);
  flex-shrink: 0;
}

.thinking-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.thinking-chevron {
  color: var(--color-text-muted);
  font-size: 12px;
}

.thinking-body {
  padding: 0 12px 12px;
  border-top: 1px solid rgba(15, 28, 46, 0.06);
}

.thinking-text {
  margin: 10px 0 0;
  padding: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.65;
  color: var(--color-text-secondary);
  background: transparent;
}

.answer-body {
  padding: 12px 14px;
  border-radius: 14px;
  border-top-left-radius: 4px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: var(--shadow-soft);
}

.markdown-body {
  color: var(--color-text);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.markdown-body :deep(p) {
  margin: 0 0 0.28em;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(p:empty) {
  display: none;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 0.5em 0 0.2em;
  font-weight: 700;
  line-height: 1.35;
  color: var(--color-text);
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child),
.markdown-body :deep(p:first-child) {
  margin-top: 0;
}

.markdown-body :deep(h1) { font-size: 1.12em; }
.markdown-body :deep(h2) { font-size: 1.06em; }
.markdown-body :deep(h3) { font-size: 1.02em; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.15em 0 0.3em;
  padding-left: 1.25em;
}

.markdown-body :deep(li) {
  margin: 0;
  padding: 0.12em 0;
  line-height: 1.6;
}

.markdown-body :deep(li + li) {
  margin-top: 0.18em;
}

.markdown-body :deep(li > p) {
  margin: 0 !important;
}

.markdown-body :deep(li > p + p) {
  margin-top: 0.12em !important;
}

.markdown-body :deep(li > ul),
.markdown-body :deep(li > ol) {
  margin: 0.1em 0 0.12em;
  padding-left: 1.1em;
}

.markdown-body :deep(ul ul),
.markdown-body :deep(ol ul),
.markdown-body :deep(ul ol),
.markdown-body :deep(ol ol) {
  margin: 0.08em 0;
}

.markdown-body :deep(strong) {
  font-weight: 700;
}

.markdown-body :deep(a) {
  color: var(--accent);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.92em;
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: rgba(15, 28, 46, 0.06);
}

.markdown-body :deep(pre) {
  margin: 0.3em 0;
  padding: 8px 10px;
  overflow-x: auto;
  border-radius: 10px;
  background: rgba(15, 28, 46, 0.06);
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
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

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 36px;
}

.model-select-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  min-width: 118px;
  height: 34px;
  padding: 0 28px 0 12px;
  border: 1px solid rgba(15, 28, 46, 0.14);
  border-radius: 999px;
  background: #f4f7fb;
  color: var(--color-text);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s,
    box-shadow 0.2s;
}

.model-select-wrap:hover,
.model-select-wrap:focus-within {
  border-color: color-mix(in srgb, var(--accent) 45%, transparent);
  background: #fff;
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.model-select-icon {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--accent);
  line-height: 1;
}

.model-select {
  appearance: none;
  -webkit-appearance: none;
  border: none;
  outline: none;
  background: transparent;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 600;
  line-height: 34px;
  height: 34px;
  padding: 0;
  margin: 0;
  min-width: 72px;
  cursor: pointer;
}

.model-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.model-select-chevron {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 11px;
  color: var(--color-text-muted);
  pointer-events: none;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-left: auto;
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

/* ===== D3 历史侧栏 ===== */
.chat-layout {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 1100px;
  height: calc(100dvh - 88px - var(--safe-top) - var(--safe-bottom));
  max-height: 780px;
  gap: 0;
  animation: page-enter 0.45s ease-out;
}

.history-sidebar {
  display: flex;
  flex-direction: column;
  width: 260px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-right: none;
  border-radius: var(--radius-lg) 0 0 var(--radius-lg);
  overflow: hidden;
}

.sidebar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 12px 10px;
  border-bottom: 1px solid rgba(15, 28, 46, 0.06);
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
}

.new-chat-btn {
  border: 1px solid var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}

.new-chat-btn:hover {
  background: var(--accent);
  color: #fff;
}

.new-chat-btn:active {
  transform: scale(0.97);
}

.sidebar-hint {
  margin: 16px 12px;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.sidebar-error {
  color: #b42318;
}

.conversation-list {
  list-style: none;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  flex: 1;
}

.conversation-item {
  display: flex;
  align-items: stretch;
  gap: 2px;
  margin-bottom: 4px;
  border-radius: 10px;
}

.conversation-item.active {
  background: var(--accent-soft);
}

.conversation-main {
  flex: 1;
  min-width: 0;
  text-align: left;
  border: none;
  background: transparent;
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 10px;
}

.conversation-name {
  display: block;
  font-size: 13px;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-time {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.conversation-delete {
  width: 28px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font-size: 16px;
  border-radius: 8px;
}

.conversation-delete:hover {
  color: #b42318;
  background: rgba(180, 35, 24, 0.08);
}

.history-toggle {
  flex-shrink: 0;
  min-height: 36px;
  padding: 6px 12px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface-soft);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 13px;
}

.sidebar-backdrop {
  display: none;
}

.chat-layout .chat-room {
  max-width: none;
  flex: 1;
  height: 100%;
  max-height: none;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  animation: none;
}

@media (max-width: 959px) {
  .history-sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 5;
    border-radius: var(--radius-lg) 0 0 var(--radius-lg);
    transform: translateX(-105%);
    transition: transform 0.22s ease;
    box-shadow: var(--shadow-card);
  }

  .history-sidebar.open {
    transform: translateX(0);
  }

  .sidebar-backdrop {
    display: block;
    position: absolute;
    inset: 0;
    z-index: 4;
    border: none;
    background: rgba(15, 28, 46, 0.28);
  }

  .chat-layout .chat-room {
    border-radius: var(--radius-lg);
  }
}

@media (min-width: 960px) {
  .history-toggle {
    display: none;
  }
}

</style>
