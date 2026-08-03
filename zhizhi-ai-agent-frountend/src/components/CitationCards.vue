<template>
  <div v-if="citations?.length" class="citation-block">
    <div class="citation-label">来自知识库</div>
    <ul class="citation-list">
      <li v-for="(item, index) in citations" :key="item.chunkId || index" class="citation-item">
        <button type="button" class="citation-main" @click="toggle(index)">
          <span class="citation-index">{{ index + 1 }}</span>
          <span class="citation-meta">
            <span class="citation-title">{{ displayTitle(item) }}</span>
            <span v-if="item.filename && item.filename !== item.title" class="citation-file">
              {{ item.filename }}
            </span>
          </span>
          <span class="citation-chevron" aria-hidden="true">{{ openIndex === index ? '▴' : '▾' }}</span>
        </button>
        <p v-show="openIndex === index" class="citation-snippet">{{ item.snippet || '暂无摘要' }}</p>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  citations: {
    type: Array,
    default: () => [],
  },
})

const openIndex = ref(0)

function displayTitle(item) {
  return item?.title || item?.filename || '未命名文档'
}

function toggle(index) {
  openIndex.value = openIndex.value === index ? -1 : index
}
</script>

<style scoped>
.citation-block {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(15, 28, 46, 0.08);
}

.citation-label {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
}

.citation-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.citation-item {
  border: 1px solid rgba(31, 111, 139, 0.18);
  border-radius: 10px;
  background: rgba(31, 111, 139, 0.06);
  overflow: hidden;
}

.citation-main {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.citation-index {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--accent, var(--color-primary));
}

.citation-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.citation-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.citation-file {
  font-size: 11px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.citation-chevron {
  color: var(--color-text-muted);
  font-size: 12px;
}

.citation-snippet {
  margin: 0;
  padding: 0 12px 10px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
