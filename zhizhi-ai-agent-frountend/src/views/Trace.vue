<template>
  <div class="trace-page">
    <ParticleBackground density="light" />
    <header class="trace-header">
      <button class="back-btn" type="button" @click="$router.push('/')">返回</button>
      <h1>Trace 统计</h1>
      <button class="refresh-btn" type="button" :disabled="loading" @click="load">刷新</button>
    </header>

    <p v-if="error" class="hint error">{{ error }}</p>

    <section v-if="stats" class="stats-grid">
      <div class="stat">
        <span class="stat-label">总任务</span>
        <span class="stat-value">{{ stats.totalRuns }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">成功</span>
        <span class="stat-value">{{ stats.successRuns }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">取消</span>
        <span class="stat-value">{{ stats.cancelledRuns }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">错误</span>
        <span class="stat-value">{{ stats.errorRuns }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">Prompt Tokens</span>
        <span class="stat-value">{{ stats.totalPromptTokens }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">Completion Tokens</span>
        <span class="stat-value">{{ stats.totalCompletionTokens }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">总 Tokens</span>
        <span class="stat-value">{{ stats.totalTokens }}</span>
      </div>
      <div class="stat">
        <span class="stat-label">平均耗时</span>
        <span class="stat-value">{{ formatMs(stats.avgDurationMs) }}</span>
      </div>
    </section>

    <section class="list-section">
      <h2>最近任务</h2>
      <p v-if="loading" class="hint">加载中…</p>
      <p v-else-if="traces.length === 0" class="hint">暂无 Trace。去 Workspace 跑一轮超级智能体即可落库。</p>
      <ul v-else class="trace-list">
        <li v-for="item in traces" :key="item.traceId" class="trace-item">
          <div class="trace-main">
            <span class="trace-id" :title="item.traceId">{{ shortId(item.traceId) }}</span>
            <span :class="['status', (item.status || '').toLowerCase()]">{{ item.status }}</span>
          </div>
          <div class="trace-meta">
            <span>步数 {{ item.stepCount ?? '-' }}</span>
            <span>Token {{ item.totalTokens ?? 0 }}</span>
            <span>{{ formatMs(item.durationMs) }}</span>
            <span>{{ formatTime(item.createDate) }}</span>
          </div>
        </li>
      </ul>
    </section>
    <SiteFooter compact />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ParticleBackground from '../components/ParticleBackground.vue'
import SiteFooter from '../components/SiteFooter.vue'
import { getTraceStats, listTraces } from '../api/trace.js'

const loading = ref(false)
const error = ref('')
const stats = ref(null)
const traces = ref([])

function shortId(id) {
  if (!id) return ''
  return id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id
}

function formatMs(ms) {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(1)} s`
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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [s, list] = await Promise.all([getTraceStats(), listTraces({ limit: 40 })])
    stats.value = s
    traces.value = Array.isArray(list) ? list : []
  } catch (err) {
    error.value = err.message || '加载失败（请确认已登录且 MySQL 已启用）'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.trace-page {
  min-height: 100dvh;
  padding: 20px 20px 40px;
  position: relative;
  color: var(--color-ink, #0f1c2e);
}

.trace-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 960px;
  margin: 0 auto 20px;
}

.trace-header h1 {
  flex: 1;
  margin: 0;
  font-size: 20px;
}

.back-btn,
.refresh-btn {
  border: 1px solid rgba(15, 28, 46, 0.12);
  background: rgba(255, 255, 255, 0.8);
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
}

.stats-grid {
  position: relative;
  z-index: 1;
  max-width: 960px;
  margin: 0 auto 24px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 14px;
  padding: 14px;
  backdrop-filter: blur(12px);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #6b7785;
}

.stat-value {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  font-weight: 650;
}

.list-section {
  position: relative;
  z-index: 1;
  max-width: 960px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.82);
  border-radius: 16px;
  padding: 16px;
  backdrop-filter: blur(12px);
}

.list-section h2 {
  margin: 0 0 12px;
  font-size: 16px;
}

.hint {
  color: #6b7785;
  font-size: 13px;
}

.hint.error {
  color: #b42318;
}

.trace-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.trace-item {
  padding: 10px 0;
  border-top: 1px solid rgba(15, 28, 46, 0.06);
}

.trace-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.trace-id {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
}

.status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef2f6;
}

.status.success {
  background: #e8f7ee;
  color: #067647;
}

.status.cancelled {
  background: #fff6e5;
  color: #b54708;
}

.status.error {
  background: #fee4e2;
  color: #b42318;
}

.trace-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #6b7785;
}

@media (max-width: 800px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
