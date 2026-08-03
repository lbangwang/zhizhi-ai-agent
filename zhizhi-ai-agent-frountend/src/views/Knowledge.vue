<template>
  <div class="knowledge-page">
    <ParticleBackground density="light" />
    <div class="orb orb-a" aria-hidden="true" />
    <div class="orb orb-b" aria-hidden="true" />

    <div class="panel">
      <header class="panel-header">
        <button class="back-btn" type="button" @click="$router.push('/')">返回</button>
        <div class="header-text">
          <p class="eyebrow">Knowledge Base</p>
          <h1>知识库</h1>
          <p class="sub">上传文档 → 自动切片 → 写入 VectorStore；对话时可展示引用来源</p>
        </div>
      </header>

      <section class="upload-section">
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
          <span class="drop-title">{{ uploading ? '上传并入库中…' : '拖拽或点击上传' }}</span>
          <span class="drop-hint">支持 .md / .txt / .docx / .doc，单文件不超过 10MB</span>
        </label>
        <div class="upload-row">
          <input
            v-model.trim="title"
            class="title-input"
            type="text"
            placeholder="可选：自定义标题"
            :disabled="uploading"
          />
          <button class="refresh-btn" type="button" :disabled="loading || uploading" @click="loadList">
            刷新列表
          </button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
      </section>

      <section class="list-section">
        <div class="section-head">
          <h2>我的文档</h2>
          <span class="count">{{ documents.length }} 篇</span>
        </div>
        <p v-if="loading" class="hint">加载中…</p>
        <p v-else-if="documents.length === 0" class="hint">暂无文档，上传后即可在面试官对话中引用</p>
        <ul v-else class="doc-list">
          <li v-for="doc in documents" :key="doc.id" class="doc-item">
            <div class="doc-main">
              <strong class="doc-title">{{ doc.title || doc.filename }}</strong>
              <span class="doc-meta">
                {{ doc.filename }} · {{ doc.chunkCount || 0 }} 切片 · {{ formatTime(doc.updateDate) }}
              </span>
            </div>
            <button
              class="delete-btn"
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

      <section class="retrieve-section">
        <div class="section-head">
          <h2>试检索</h2>
        </div>
        <div class="retrieve-row">
          <input
            v-model.trim="retrieveQuery"
            class="retrieve-input"
            type="text"
            placeholder="输入问题，预览会命中哪些片段"
            @keydown.enter.prevent="onRetrieve"
          />
          <button class="primary-btn" type="button" :disabled="retrieving || !retrieveQuery" @click="onRetrieve">
            {{ retrieving ? '检索中…' : '检索' }}
          </button>
        </div>
        <CitationCards v-if="citations.length" :citations="citations" />
        <p v-else-if="retrieveTried && !retrieving" class="hint">未命中片段，试试换个问法或先上传相关文档</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ParticleBackground from '../components/ParticleBackground.vue'
import CitationCards from '../components/CitationCards.vue'
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
  display: flex;
  justify-content: center;
  padding:
    calc(20px + var(--safe-top))
    calc(16px + var(--safe-right))
    calc(20px + var(--safe-bottom))
    calc(16px + var(--safe-left));
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(56px);
  pointer-events: none;
  z-index: 0;
}

.orb-a {
  width: min(320px, 50vw);
  height: min(320px, 50vw);
  top: -8%;
  left: -6%;
  background: rgba(31, 111, 139, 0.28);
}

.orb-b {
  width: min(280px, 45vw);
  height: min(280px, 45vw);
  right: -8%;
  bottom: 10%;
  background: rgba(47, 122, 107, 0.2);
}

.panel {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 760px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  animation: page-enter 0.4s ease-out;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.back-btn {
  flex-shrink: 0;
  min-height: 36px;
  margin-top: 4px;
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface-soft);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 13px;
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-primary);
}

.header-text h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 4vw, 2rem);
  color: var(--color-ink);
}

.sub {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.upload-section,
.list-section,
.retrieve-section {
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.68));
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  padding: 16px;
}

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 120px;
  border: 1.5px dashed rgba(31, 111, 139, 0.35);
  border-radius: var(--radius-md);
  background: rgba(31, 111, 139, 0.05);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s;
}

.dropzone.dragging {
  border-color: var(--color-primary);
  background: rgba(31, 111, 139, 0.12);
}

.dropzone.disabled {
  opacity: 0.65;
  cursor: wait;
}

.file-input {
  display: none;
}

.drop-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-ink);
}

.drop-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.upload-row,
.retrieve-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.title-input,
.retrieve-input {
  flex: 1;
  min-width: 0;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(15, 28, 46, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.refresh-btn,
.primary-btn,
.delete-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid rgba(31, 111, 139, 0.25);
  background: rgba(31, 111, 139, 0.08);
  color: var(--color-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.primary-btn:disabled,
.refresh-btn:disabled,
.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error {
  margin: 10px 0 0;
  color: #b42318;
  font-size: 13px;
}

.success {
  margin: 10px 0 0;
  color: #17663f;
  font-size: 13px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  font-size: 16px;
  color: var(--color-ink);
}

.count {
  font-size: 12px;
  color: var(--color-text-muted);
}

.hint {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
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
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(244, 248, 252, 0.8);
  border: 1px solid rgba(15, 28, 46, 0.06);
}

.doc-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.doc-title {
  font-size: 14px;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 12px;
  color: var(--color-text-muted);
}

.delete-btn {
  height: 32px;
  padding: 0 10px;
  color: #b42318;
  background: rgba(180, 35, 24, 0.06);
  border-color: rgba(180, 35, 24, 0.18);
}

.retrieve-section :deep(.citation-block) {
  margin-top: 12px;
}

@keyframes page-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 640px) {
  .upload-row,
  .retrieve-row {
    flex-direction: column;
  }

  .refresh-btn,
  .primary-btn {
    width: 100%;
  }
}
</style>
