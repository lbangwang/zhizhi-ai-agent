<template>
  <div class="knowledge-page">
    <ParticleBackground density="light" />

    <div class="shell">
      <header class="topbar">
        <button class="btn ghost" type="button" @click="$router.push('/')">← 返回</button>
        <div class="brand-row">
          <img class="brand-icon" :src="APP_AVATARS.knowledge" alt="" />
          <div>
            <h1>知识库</h1>
            <p class="sub">上传文档 → 自动切片 → 写入向量库；对话时可展示引用来源</p>
          </div>
        </div>
        <button
          class="btn ghost"
          type="button"
          :disabled="loading || uploading"
          @click="loadList"
        >
          刷新
        </button>
      </header>

      <div class="stack">
        <section class="card">
          <div class="card-head">
            <span class="step">1</span>
            <div>
              <h2>上传文档</h2>
              <p>支持 Markdown / 文本 / Word，单文件不超过 10MB</p>
            </div>
          </div>

          <label
            class="dropzone"
            :class="{ dragging, disabled: uploading }"
            @dragenter.prevent="dragging = true"
            @dragover.prevent="dragging = true"
            @dragleave.prevent="dragging = false"
            @drop.prevent="onDrop"
          >
            <input
              class="file-input"
              type="file"
              accept=".md,.markdown,.txt,.doc,.docx,text/plain,text/markdown,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              :disabled="uploading"
              @change="onFileChange"
            />
            <span class="drop-icon" aria-hidden="true">↑</span>
            <span class="drop-title">{{ uploading ? '上传并入库中…' : '拖拽到此处，或点击选择文件' }}</span>
            <span class="drop-hint">.md · .txt · .docx · .doc</span>
          </label>

          <div class="upload-row">
            <input
              v-model.trim="title"
              class="field"
              type="text"
              placeholder="可选：自定义标题"
              :disabled="uploading"
            />
          </div>
          <p v-if="error" class="msg error">{{ error }}</p>
          <p v-if="success" class="msg success">{{ success }}</p>
        </section>

        <section class="card">
          <div class="card-head">
            <span class="step">2</span>
            <div>
              <h2>我的文档</h2>
              <p>已入库并可被对话检索引用</p>
            </div>
            <span class="count">{{ documents.length }} 篇</span>
          </div>

          <p v-if="loading" class="hint">加载中…</p>
          <div v-else-if="documents.length === 0" class="empty">
            <p>暂无文档</p>
            <span>上传后即可在面试官对话中展示引用卡片</span>
          </div>
          <ul v-else class="doc-list">
            <li v-for="doc in documents" :key="doc.id" class="doc-item">
              <div class="doc-badge" aria-hidden="true">{{ fileBadge(doc.filename) }}</div>
              <div class="doc-main">
                <strong class="doc-title">{{ doc.title || doc.filename }}</strong>
                <span class="doc-meta">
                  <span>{{ doc.filename }}</span>
                  <span>{{ doc.chunkCount || 0 }} 切片</span>
                  <span>{{ formatTime(doc.updateDate) }}</span>
                </span>
              </div>
              <button
                class="btn danger"
                type="button"
                title="删除"
                :disabled="deletingId === doc.id"
                @click="onDelete(doc)"
              >
                {{ deletingId === doc.id ? '…' : '删除' }}
              </button>
            </li>
          </ul>
        </section>

        <section class="card">
          <div class="card-head">
            <span class="step">3</span>
            <div>
              <h2>试检索</h2>
              <p>预览问题会命中哪些片段</p>
            </div>
          </div>

          <div class="retrieve-row">
            <input
              v-model.trim="retrieveQuery"
              class="field"
              type="text"
              placeholder="输入问题，例如：文档里提到的 MCP 是什么？"
              @keydown.enter.prevent="onRetrieve"
            />
            <button
              class="btn primary"
              type="button"
              :disabled="retrieving || !retrieveQuery"
              @click="onRetrieve"
            >
              {{ retrieving ? '检索中…' : '检索' }}
            </button>
          </div>

          <CitationCards v-if="citations.length" :citations="citations" />
          <p v-else-if="retrieveTried && !retrieving" class="hint retrieve-hint">
            未命中片段，试试换个问法或先上传相关文档
          </p>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ParticleBackground from '../components/ParticleBackground.vue'
import CitationCards from '../components/CitationCards.vue'
import { APP_AVATARS } from '../constants/apps.js'
import {
  deleteDocument,
  listDocuments,
  retrieveKnowledge,
  uploadDocument,
} from '../api/knowledge.js'

const documents = ref([])
const loading = ref(false)
const uploading = ref(false)
const deletingId = ref('')
const dragging = ref(false)
const title = ref('')
const error = ref('')
const success = ref('')

const retrieveQuery = ref('')
const citations = ref([])
const retrieving = ref(false)
const retrieveTried = ref(false)

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

function fileBadge(filename = '') {
  const name = String(filename).toLowerCase()
  if (name.endsWith('.md') || name.endsWith('.markdown')) return 'MD'
  if (name.endsWith('.txt')) return 'TXT'
  if (name.endsWith('.docx') || name.endsWith('.doc')) return 'DOC'
  return 'FILE'
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const list = await listDocuments()
    documents.value = Array.isArray(list) ? list : []
  } catch (err) {
    error.value = err.message || '加载失败'
    documents.value = []
  } finally {
    loading.value = false
  }
}

async function uploadFile(file) {
  if (!file || uploading.value) return
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const doc = await uploadDocument(file, title.value || undefined)
    success.value = `已入库「${doc.title || doc.filename}」，共 ${doc.chunkCount || 0} 个切片`
    title.value = ''
    await loadList()
  } catch (err) {
    error.value = err.message || '上传失败'
  } finally {
    uploading.value = false
    dragging.value = false
  }
}

function onFileChange(event) {
  const file = event.target?.files?.[0]
  event.target.value = ''
  uploadFile(file)
}

function onDrop(event) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  uploadFile(file)
}

async function onDelete(doc) {
  if (!doc?.id || deletingId.value) return
  if (!window.confirm(`确认删除「${doc.title || doc.filename}」？向量切片将一并移除。`)) return
  deletingId.value = doc.id
  error.value = ''
  try {
    await deleteDocument(doc.id)
    success.value = '已删除'
    await loadList()
  } catch (err) {
    error.value = err.message || '删除失败'
  } finally {
    deletingId.value = ''
  }
}

async function onRetrieve() {
  if (!retrieveQuery.value || retrieving.value) return
  retrieving.value = true
  retrieveTried.value = true
  error.value = ''
  try {
    const data = await retrieveKnowledge({ query: retrieveQuery.value, topK: 4 })
    citations.value = Array.isArray(data?.citations) ? data.citations : []
  } catch (err) {
    error.value = err.message || '检索失败'
    citations.value = []
  } finally {
    retrieving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.knowledge-page {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  min-height: 100dvh;
  padding:
    calc(16px + var(--safe-top))
    calc(var(--page-padding-x) + var(--safe-right))
    calc(20px + var(--safe-bottom))
    calc(var(--page-padding-x) + var(--safe-left));
}

.shell {
  position: relative;
  z-index: 1;
  width: min(720px, 100%);
  margin: 0 auto;
  animation: page-enter 0.4s ease-out;
}

.topbar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  flex-shrink: 0;
}

.brand-row h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(22px, 3vw, 28px);
  font-weight: 700;
  color: var(--color-ink);
  line-height: 1.15;
}

.sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.45;
}

.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-md);
  padding: 18px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.card-head h2 {
  margin: 0 0 4px;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 650;
  color: var(--color-ink);
}

.card-head p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.step {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(145deg, #1f6f8b, #2f7a6b);
}

.count {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  align-self: center;
}

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 132px;
  border: 1.5px dashed rgba(31, 111, 139, 0.32);
  border-radius: var(--radius-md);
  background: rgba(31, 111, 139, 0.04);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.dropzone:hover,
.dropzone.dragging {
  border-color: var(--color-primary);
  background: rgba(31, 111, 139, 0.1);
}

.dropzone.disabled {
  opacity: 0.65;
  cursor: wait;
}

.file-input {
  display: none;
}

.drop-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(31, 111, 139, 0.15);
}

.drop-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
}

.drop-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.upload-row {
  margin-top: 12px;
}

.field {
  width: 100%;
  min-width: 0;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(15, 28, 46, 0.1);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  color: var(--color-text);
}

.field:focus {
  outline: none;
  border-color: rgba(31, 111, 139, 0.45);
  box-shadow: 0 0 0 3px rgba(31, 111, 139, 0.12);
}

.btn {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  min-height: 36px;
  padding: 0 14px;
  transition: background 0.2s ease, border-color 0.2s ease, opacity 0.2s ease;
}

.btn.ghost {
  background: rgba(255, 255, 255, 0.55);
  border-color: rgba(18, 38, 58, 0.1);
  color: var(--color-text-secondary);
}

.btn.ghost:hover:not(:disabled) {
  border-color: rgba(31, 111, 139, 0.3);
  color: var(--color-primary);
}

.btn.primary {
  background: var(--color-primary);
  color: #fff;
}

.btn.primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn.danger {
  flex-shrink: 0;
  min-height: 32px;
  padding: 0 12px;
  color: #b42318;
  background: rgba(180, 35, 24, 0.06);
  border-color: rgba(180, 35, 24, 0.16);
}

.btn.danger:hover:not(:disabled) {
  background: rgba(180, 35, 24, 0.1);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.msg {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.45;
}

.msg.error {
  color: #b42318;
}

.msg.success {
  color: #17663f;
}

.hint {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.empty {
  padding: 28px 16px;
  text-align: center;
  border-radius: var(--radius-sm);
  background: rgba(244, 248, 252, 0.7);
  border: 1px dashed rgba(15, 28, 46, 0.1);
}

.empty p {
  margin: 0 0 4px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.empty span {
  font-size: 13px;
  color: var(--color-text-muted);
}

.doc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: rgba(244, 248, 252, 0.75);
  border: 1px solid rgba(15, 28, 46, 0.06);
}

.doc-badge {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(31, 111, 139, 0.15);
}

.doc-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.doc-title {
  font-size: 14px;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.retrieve-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.retrieve-row .field {
  flex: 1;
}

.retrieve-row .btn.primary {
  flex-shrink: 0;
  min-height: 40px;
}

.retrieve-hint {
  margin-top: 12px;
}

:deep(.citation-block) {
  margin-top: 14px;
}

@media (max-width: 640px) {
  .topbar {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'back refresh'
      'brand brand';
  }

  .topbar .btn.ghost:first-child {
    grid-area: back;
    justify-self: start;
  }

  .topbar .btn.ghost:last-child {
    grid-area: refresh;
    justify-self: end;
  }

  .brand-row {
    grid-area: brand;
  }

  .doc-item {
    flex-wrap: wrap;
  }

  .btn.danger {
    margin-left: auto;
  }

  .retrieve-row {
    flex-direction: column;
    align-items: stretch;
  }

  .retrieve-row .btn.primary {
    width: 100%;
  }
}
</style>
