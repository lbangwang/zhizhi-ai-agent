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
            <p class="sub">选文件调参预览 → 确认入库 → 对话可引用</p>
          </div>
        </div>
        <button
          class="btn ghost"
          type="button"
          :disabled="loading || uploading || previewing"
          @click="loadList"
        >
          刷新
        </button>
      </header>

      <div class="layout">
        <!-- 左栏：上传 + 文档 + 试检索 -->
        <aside ref="sideRef" class="col col-side">
          <section class="card">
            <div class="card-head">
              <span class="step">1</span>
              <div>
                <h2>上传</h2>
                <p>.md / .txt / Word，≤10MB</p>
              </div>
            </div>

            <label
              class="dropzone"
              :class="{ dragging, disabled: uploading || previewing, active: !!selectedFile }"
              @dragenter.prevent="dragging = true"
              @dragover.prevent="dragging = true"
              @dragleave.prevent="dragging = false"
              @drop.prevent="onDrop"
            >
              <input
                class="file-input"
                type="file"
                accept=".md,.markdown,.txt,.doc,.docx,text/plain,text/markdown,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                :disabled="uploading || previewing"
                @change="onFileChange"
              />
              <span class="drop-icon" aria-hidden="true">↑</span>
              <span class="drop-title">
                {{
                  previewing
                    ? '切片预览中…'
                    : uploading
                      ? '入库中…'
                      : selectedFile
                        ? selectedFile.name
                        : '拖拽或点击选择'
                }}
              </span>
            </label>

            <input
              v-model.trim="title"
              class="field mt"
              type="text"
              placeholder="可选标题"
              :disabled="uploading"
            />

            <div class="params">
              <label class="param">
                <span>策略</span>
                <select v-model="splitStrategy" class="field" :disabled="uploading || previewing">
                  <option value="paragraph">按段落</option>
                  <option value="token">按 Token</option>
                </select>
              </label>
              <label class="param">
                <span>Token</span>
                <input
                  v-model.number="chunkTokenSize"
                  class="field"
                  type="number"
                  min="50"
                  max="4000"
                  step="50"
                  :disabled="uploading || previewing"
                />
              </label>
              <label class="param">
                <span>段最大字</span>
                <input
                  v-model.number="paragraphMaxChars"
                  class="field"
                  type="number"
                  min="100"
                  max="20000"
                  step="50"
                  :disabled="uploading || previewing || splitStrategy === 'token'"
                />
              </label>
              <label class="param">
                <span>短段合并</span>
                <input
                  v-model.number="paragraphMinMergeChars"
                  class="field"
                  type="number"
                  min="0"
                  max="2000"
                  step="10"
                  :disabled="uploading || previewing || splitStrategy === 'token'"
                />
              </label>
            </div>

            <div class="action-row">
              <button
                class="btn ghost grow"
                type="button"
                :disabled="!selectedFile || previewing || uploading"
                @click="runPreview"
              >
                {{ previewing ? '预览中…' : '重新预览' }}
              </button>
              <button
                class="btn primary grow"
                type="button"
                :disabled="!selectedFile || !preview || previewing || uploading"
                @click="confirmUpload"
              >
                {{ uploading ? '入库中…' : '确认入库' }}
              </button>
            </div>
            <button
              v-if="selectedFile"
              class="btn ghost clear-btn"
              type="button"
              :disabled="uploading || previewing"
              @click="clearSelection"
            >
              清除所选文件
            </button>

            <p v-if="error" class="msg error">{{ error }}</p>
            <p v-if="success" class="msg success">{{ success }}</p>
          </section>

          <section class="card card-docs">
            <div class="card-head">
              <span class="step">2</span>
              <div>
                <h2>文档</h2>
                <p>点「切片」在右侧查看</p>
              </div>
              <span class="count">{{ documents.length }}</span>
            </div>

            <p v-if="loading" class="hint">加载中…</p>
            <div v-else-if="documents.length === 0" class="empty compact">
              <p>暂无文档</p>
            </div>
            <ul v-else class="doc-list">
              <li
                v-for="doc in documents"
                :key="doc.id"
                class="doc-item"
                :class="{ active: viewingDocId === doc.id }"
              >
                <div class="doc-badge" aria-hidden="true">{{ fileBadge(doc.filename) }}</div>
                <div class="doc-main">
                  <strong class="doc-title">{{ doc.title || doc.filename }}</strong>
                  <span class="doc-meta">
                    {{ doc.chunkCount || 0 }} 片 · {{ formatTime(doc.updateDate) }}
                  </span>
                </div>
                <div class="doc-actions">
                  <button
                    class="btn ghost sm"
                    type="button"
                    :disabled="chunksLoadingId === doc.id"
                    @click="toggleStoredChunks(doc)"
                  >
                    {{
                      chunksLoadingId === doc.id
                        ? '…'
                        : viewingDocId === doc.id
                          ? '收起'
                          : '切片'
                    }}
                  </button>
                  <button
                    class="btn danger sm"
                    type="button"
                    :disabled="deletingId === doc.id"
                    @click="onDelete(doc)"
                  >
                    {{ deletingId === doc.id ? '…' : '删' }}
                  </button>
                </div>
              </li>
            </ul>
          </section>

          <section class="card">
            <div class="card-head">
              <span class="step">3</span>
              <div>
                <h2>试检索</h2>
                <p>看看问题会命中哪些片段</p>
              </div>
            </div>

            <div class="retrieve-row">
              <input
                v-model.trim="retrieveQuery"
                class="field"
                type="text"
                placeholder="例如：文档里提到的 MCP 是什么？"
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
              未命中，换个问法或先入库相关文档
            </p>
          </section>
        </aside>

        <!-- 右栏：切片预览（宽度与左侧平分，高度对齐左侧，内部滚动） -->
        <section
          ref="previewRef"
          class="card card-chunks col-preview"
          :style="previewStyle"
        >
          <div class="card-head">
            <span class="step">{{ previewMode === 'stored' ? '库' : '切' }}</span>
            <div>
              <h2>{{ previewTitle }}</h2>
              <p>{{ previewSubtitle }}</p>
            </div>
          </div>

          <div v-if="previewMode === 'preview' && preview" class="preview-summary">
            <span>{{ preview.filename }}</span>
            <span>{{ preview.extractedCharCount || 0 }} 字</span>
            <span>{{ preview.strategy }}</span>
            <span>{{ preview.chunkCount || 0 }} 片</span>
            <span v-if="preview.truncated" class="warn">仅前 {{ preview.chunks?.length || 0 }} 片</span>
          </div>

          <div v-if="previewMode === 'stored' && storedChunks?.truncated" class="preview-summary">
            <span>共 {{ storedChunks.chunkCount }} 片</span>
            <span class="warn">仅展示前 {{ storedChunks.chunks?.length || 0 }} 片</span>
          </div>

          <p v-if="previewMode === 'stored' && storedChunksError" class="msg error">
            {{ storedChunksError }}
          </p>

          <div class="chunk-scroll">
            <div v-if="displayChunks.length" class="chunk-list">
              <details
                v-for="chunk in displayChunks"
                :key="chunk.chunkId || `p-${chunk.index}`"
                class="chunk-item"
              >
                <summary>
                  <span class="chunk-index">#{{ (chunk.index ?? 0) + 1 }}</span>
                  <span class="chunk-len">{{ chunk.charCount || 0 }} 字</span>
                  <span class="chunk-snip">{{ snippet(chunk.text) }}</span>
                </summary>
                <pre class="chunk-body">{{ chunk.text }}</pre>
              </details>
            </div>

            <div v-else class="empty panel-empty">
              <p v-if="previewing || chunksLoadingId">加载切片中…</p>
              <template v-else>
                <p>切片预览区</p>
                <span>左侧选文件调参后在此预览；点文档「切片」查看已入库片段</span>
              </template>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import ParticleBackground from '../components/ParticleBackground.vue'
import CitationCards from '../components/CitationCards.vue'
import { APP_AVATARS } from '../constants/apps.js'
import {
  deleteDocument,
  listChunks,
  listDocuments,
  previewSplit,
  retrieveKnowledge,
  uploadDocument,
} from '../api/knowledge.js'

const documents = ref([])
const loading = ref(false)
const uploading = ref(false)
const previewing = ref(false)
const deletingId = ref('')
const dragging = ref(false)
const title = ref('')
const error = ref('')
const success = ref('')

const selectedFile = ref(null)
const preview = ref(null)

const splitStrategy = ref('paragraph')
const chunkTokenSize = ref(400)
const paragraphMaxChars = ref(800)
const paragraphMinMergeChars = ref(80)

const viewingDocId = ref('')
const viewingDocTitle = ref('')
const storedChunks = ref(null)
const storedChunksError = ref('')
const chunksLoadingId = ref('')

const retrieveQuery = ref('')
const citations = ref([])
const retrieving = ref(false)
const retrieveTried = ref(false)

const sideRef = ref(null)
const previewRef = ref(null)
const previewHeight = ref(null)
let sideObserver = null

const previewStyle = computed(() => {
  if (!previewHeight.value) return undefined
  return {
    height: `${previewHeight.value}px`,
  }
})

function syncPreviewHeight() {
  const side = sideRef.value
  if (!side) return
  const h = Math.round(side.getBoundingClientRect().height)
  if (h > 0) previewHeight.value = h
}

const previewMode = computed(() => {
  if (viewingDocId.value) return 'stored'
  if (preview.value?.chunks?.length || preview.value) return 'preview'
  return 'empty'
})

const previewTitle = computed(() => {
  if (previewMode.value === 'stored') return '已入库切片'
  return '切片预览'
})

const previewSubtitle = computed(() => {
  if (previewMode.value === 'stored') {
    return viewingDocTitle.value || '只读查看'
  }
  if (previewMode.value === 'preview') {
    return '调参后点「重新预览」，满意再入库'
  }
  return '预览与入库切片会显示在这里'
})

const displayChunks = computed(() => {
  if (previewMode.value === 'stored') {
    return Array.isArray(storedChunks.value?.chunks) ? storedChunks.value.chunks : []
  }
  if (previewMode.value === 'preview') {
    return Array.isArray(preview.value?.chunks) ? preview.value.chunks : []
  }
  return []
})

function currentSplitParams() {
  return {
    splitStrategy: splitStrategy.value,
    chunkTokenSize: chunkTokenSize.value,
    paragraphMaxChars: paragraphMaxChars.value,
    paragraphMinMergeChars: paragraphMinMergeChars.value,
  }
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

function fileBadge(filename = '') {
  const name = String(filename).toLowerCase()
  if (name.endsWith('.md') || name.endsWith('.markdown')) return 'MD'
  if (name.endsWith('.txt')) return 'TXT'
  if (name.endsWith('.docx') || name.endsWith('.doc')) return 'DOC'
  return 'FILE'
}

function snippet(text = '', max = 56) {
  const normalized = String(text).replace(/\s+/g, ' ').trim()
  if (!normalized) return '（空切片）'
  return normalized.length > max ? `${normalized.slice(0, max)}…` : normalized
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

async function runPreview() {
  if (!selectedFile.value || previewing.value || uploading.value) return
  viewingDocId.value = ''
  viewingDocTitle.value = ''
  storedChunks.value = null
  previewing.value = true
  error.value = ''
  success.value = ''
  try {
    preview.value = await previewSplit(selectedFile.value, currentSplitParams())
  } catch (err) {
    preview.value = null
    error.value = err.message || '切片预览失败'
  } finally {
    previewing.value = false
    dragging.value = false
  }
}

async function confirmUpload() {
  if (!selectedFile.value || uploading.value) return
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const doc = await uploadDocument(
      selectedFile.value,
      title.value || undefined,
      currentSplitParams(),
    )
    success.value = `已入库「${doc.title || doc.filename}」，共 ${doc.chunkCount || 0} 个切片`
    clearSelection()
    title.value = ''
    await loadList()
  } catch (err) {
    error.value = err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

function clearSelection() {
  selectedFile.value = null
  preview.value = null
}

function selectFile(file) {
  if (!file) return
  selectedFile.value = file
  preview.value = null
  viewingDocId.value = ''
  viewingDocTitle.value = ''
  storedChunks.value = null
  error.value = ''
  success.value = ''
  runPreview()
}

function onFileChange(event) {
  const file = event.target?.files?.[0]
  event.target.value = ''
  selectFile(file)
}

function onDrop(event) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  selectFile(file)
}

async function toggleStoredChunks(doc) {
  if (!doc?.id) return
  if (viewingDocId.value === doc.id) {
    viewingDocId.value = ''
    viewingDocTitle.value = ''
    storedChunks.value = null
    storedChunksError.value = ''
    return
  }
  viewingDocId.value = doc.id
  viewingDocTitle.value = doc.title || doc.filename || ''
  storedChunks.value = null
  storedChunksError.value = ''
  chunksLoadingId.value = doc.id
  try {
    storedChunks.value = await listChunks(doc.id)
  } catch (err) {
    storedChunksError.value = err.message || '加载切片失败'
  } finally {
    chunksLoadingId.value = ''
  }
}

async function onDelete(doc) {
  if (!doc?.id || deletingId.value) return
  if (!window.confirm(`确认删除「${doc.title || doc.filename}」？向量切片将一并移除。`)) return
  deletingId.value = doc.id
  error.value = ''
  try {
    await deleteDocument(doc.id)
    success.value = '已删除'
    if (viewingDocId.value === doc.id) {
      viewingDocId.value = ''
      viewingDocTitle.value = ''
      storedChunks.value = null
    }
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

onMounted(() => {
  loadList()
  nextTick(() => {
    syncPreviewHeight()
    if (typeof ResizeObserver !== 'undefined' && sideRef.value) {
      sideObserver = new ResizeObserver(() => syncPreviewHeight())
      sideObserver.observe(sideRef.value)
    }
    window.addEventListener('resize', syncPreviewHeight)
  })
})

onUnmounted(() => {
  sideObserver?.disconnect()
  window.removeEventListener('resize', syncPreviewHeight)
})
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
    calc(24px + var(--safe-bottom))
    calc(var(--page-padding-x) + var(--safe-left));
}

.shell {
  position: relative;
  z-index: 1;
  width: min(960px, 100%);
  margin: 0 auto;
  animation: page-enter 0.4s ease-out;
}

.topbar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  flex-shrink: 0;
}

.brand-row h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(20px, 2.6vw, 24px);
  font-weight: 700;
  color: var(--color-ink);
  line-height: 1.15;
}

.sub {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  align-items: start;
}

.col-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.col-preview {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.card {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-md);
  padding: 14px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.card-chunks .card-head,
.card-chunks .preview-summary,
.card-chunks .msg {
  flex-shrink: 0;
}

.chunk-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding-right: 2px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
}

.card-head h2 {
  margin: 0 0 2px;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 650;
  color: var(--color-ink);
}

.card-head p {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.35;
}

.step {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: 12px;
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
  padding: 3px 9px;
  border-radius: var(--radius-sm);
  align-self: center;
}

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 96px;
  padding: 12px;
  border: 1.5px dashed rgba(31, 111, 139, 0.32);
  border-radius: var(--radius-md);
  background: rgba(31, 111, 139, 0.04);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.dropzone:hover,
.dropzone.dragging,
.dropzone.active {
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
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(31, 111, 139, 0.15);
}

.drop-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink);
  text-align: center;
  word-break: break-all;
  line-height: 1.35;
}

.params {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.param {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.action-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.action-row .grow {
  flex: 1;
}

.clear-btn {
  width: 100%;
  margin-top: 8px;
}

.mt {
  margin-top: 10px;
}

.preview-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.preview-summary .warn {
  color: #a15c00;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chunk-item {
  border: 1px solid rgba(15, 28, 46, 0.08);
  border-radius: var(--radius-sm);
  background: rgba(244, 248, 252, 0.75);
  padding: 0 10px;
}

.chunk-item summary {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  cursor: pointer;
  list-style: none;
}

.chunk-item summary::-webkit-details-marker {
  display: none;
}

.chunk-index {
  flex-shrink: 0;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary);
}

.chunk-len {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-muted);
}

.chunk-snip {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.chunk-body {
  margin: 0 0 10px;
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 28, 46, 0.06);
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--color-text);
  max-height: 220px;
  overflow: auto;
}

.field {
  width: 100%;
  min-width: 0;
  height: 36px;
  padding: 0 10px;
  border: 1px solid rgba(15, 28, 46, 0.1);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  color: var(--color-text);
}

.field:focus {
  outline: none;
  border-color: rgba(31, 111, 139, 0.45);
  box-shadow: 0 0 0 3px rgba(31, 111, 139, 0.12);
}

select.field {
  cursor: pointer;
}

.btn {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  min-height: 34px;
  padding: 0 12px;
  transition: background 0.2s ease, border-color 0.2s ease, opacity 0.2s ease;
}

.btn.sm {
  min-height: 28px;
  padding: 0 8px;
  font-size: 12px;
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
  font-size: 12px;
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
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.empty {
  padding: 20px 12px;
  text-align: center;
  border-radius: var(--radius-sm);
  background: rgba(244, 248, 252, 0.7);
  border: 1px dashed rgba(15, 28, 46, 0.1);
}

.empty.compact {
  padding: 16px 10px;
}

.empty p {
  margin: 0 0 4px;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.empty span {
  font-size: 12px;
  color: var(--color-text-muted);
}

.panel-empty {
  min-height: 160px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.doc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: min(36vh, 320px);
  overflow: auto;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: var(--radius-sm);
  background: rgba(244, 248, 252, 0.75);
  border: 1px solid rgba(15, 28, 46, 0.06);
}

.doc-item.active {
  border-color: rgba(31, 111, 139, 0.35);
  background: rgba(31, 111, 139, 0.08);
}

.doc-badge {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(31, 111, 139, 0.15);
}

.doc-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.doc-title {
  font-size: 13px;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.doc-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.retrieve-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.retrieve-row .btn.primary {
  width: 100%;
  min-height: 36px;
}

.retrieve-hint {
  margin-top: 10px;
}

:deep(.citation-block) {
  margin-top: 12px;
}

@media (max-width: 860px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .col-side {
    order: 1;
  }

  .col-preview {
    order: 2;
    height: 360px !important;
  }

  .doc-list {
    max-height: none;
  }

  .retrieve-row {
    flex-direction: column;
  }
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

  .params {
    grid-template-columns: 1fr;
  }
}
</style>
