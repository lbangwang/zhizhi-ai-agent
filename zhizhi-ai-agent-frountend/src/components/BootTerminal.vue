<template>
  <div class="boot-shell" role="img" :aria-label="ariaLabel">
    <div class="boot-terminal">
      <div class="shine" aria-hidden="true" />
      <div class="terminal-chrome">
        <div class="traffic" aria-hidden="true">
          <span class="dot love" />
          <span class="dot soft" />
          <span class="dot agent" />
        </div>
        <span class="terminal-title">{{ title }}</span>
        <span class="status-pill">LIVE</span>
      </div>

      <div ref="bodyRef" class="terminal-body">
        <div v-for="(line, index) in visibleLines" :key="`${index}-${line.text}`" :class="['line', line.type]">
          <template v-if="line.type === 'cmd'">
            <span class="prompt">{{ prompt }}</span>
            <span class="cmd-text">{{ line.display }}</span>
            <span v-if="typingIndex === index" class="cursor">▋</span>
          </template>
          <template v-else>
            <span class="msg-text">{{ line.display }}</span>
            <span v-if="typingIndex === index" class="cursor">▋</span>
          </template>
        </div>
        <div v-if="done && showIdleCursor" class="line idle">
          <span class="prompt">{{ prompt }}</span>
          <span class="cursor">▋</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { BRAND_NAME } from '../constants/apps.js'

defineProps({
  title: {
    type: String,
    default: 'zhizhi-terminal',
  },
  prompt: {
    type: String,
    default: 'root@zhizhi:~$',
  },
})

const script = [
  { type: 'cmd', text: './launch-zhizhi-ai.sh' },
  { type: 'info', text: '正在初始化 AI 引擎...' },
  { type: 'info', text: '加载 AI面试官小助手CC 与超级智能体模型...' },
  { type: 'info', text: '连接云端服务...' },
  { type: 'success', text: `${BRAND_NAME} 启动成功！选择下方应用开始体验` },
]

const visibleLines = ref([])
const typingIndex = ref(-1)
const done = ref(false)
const showIdleCursor = ref(true)
const bodyRef = ref(null)

const ariaLabel = computed(
  () => `${BRAND_NAME} 启动终端动画：${script.map((item) => item.text).join(' ')}`,
)

let timers = []
let reducedMotion = false

function wait(ms) {
  return new Promise((resolve) => {
    const id = setTimeout(resolve, ms)
    timers.push(id)
  })
}

function scrollBottom() {
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

async function typeLine(line, index) {
  typingIndex.value = index
  const entry = { ...line, display: '' }
  visibleLines.value.push(entry)

  if (reducedMotion) {
    entry.display = line.text
    typingIndex.value = -1
    scrollBottom()
    return
  }

  for (let i = 0; i < line.text.length; i++) {
    entry.display = line.text.slice(0, i + 1)
    scrollBottom()
    await wait(line.type === 'cmd' ? 28 : 18)
  }

  typingIndex.value = -1
  await wait(line.type === 'cmd' ? 320 : 220)
}

async function run() {
  visibleLines.value = []
  done.value = false

  for (let i = 0; i < script.length; i++) {
    await typeLine(script[i], i)
  }

  done.value = true
}

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  run()
})

onUnmounted(() => {
  timers.forEach(clearTimeout)
  timers = []
})
</script>

<style scoped>
.boot-shell {
  width: 100%;
  margin: 0 0 clamp(28px, 4.5vw, 40px);
  padding: 2px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(
    120deg,
    rgba(31, 111, 139, 0.85),
    rgba(196, 92, 106, 0.75),
    rgba(95, 168, 192, 0.9),
    rgba(31, 111, 139, 0.85)
  );
  background-size: 300% 300%;
  animation:
    page-enter 0.6s ease-out,
    border-flow 6s linear infinite,
    shell-float 5.5s ease-in-out infinite;
  box-shadow:
    0 16px 40px rgba(7, 21, 37, 0.14),
    0 0 28px rgba(31, 111, 139, 0.18);
}

.boot-terminal {
  position: relative;
  width: 100%;
  border-radius: var(--radius-lg);
  overflow: hidden;
  text-align: left;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.58));
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  animation: panel-pulse 4.8s ease-in-out infinite;
}

.shine {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(
    105deg,
    transparent 35%,
    rgba(255, 255, 255, 0.55) 48%,
    transparent 62%
  );
  transform: translateX(-120%);
  animation: shine-sweep 4.2s ease-in-out infinite;
}

.terminal-chrome {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.5);
  border-bottom: 1px solid rgba(15, 28, 46, 0.06);
}

.traffic {
  position: absolute;
  left: 14px;
  display: flex;
  gap: 7px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: dot-breathe 2.4s ease-in-out infinite;
}

.dot.love {
  background: var(--color-accent-love);
  box-shadow: 0 0 10px rgba(196, 92, 106, 0.55);
  animation-delay: 0s;
}

.dot.soft {
  background: #c5d0da;
  animation-delay: 0.25s;
}

.dot.agent {
  background: var(--color-accent-agent);
  box-shadow: 0 0 10px rgba(31, 111, 139, 0.5);
  animation-delay: 0.5s;
}

.terminal-title {
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  letter-spacing: 0.06em;
}

.status-pill {
  position: absolute;
  right: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  animation: status-blink 1.8s ease-in-out infinite;
}

.terminal-body {
  position: relative;
  z-index: 1;
  min-height: 168px;
  max-height: 220px;
  padding: 16px 18px 18px;
  overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Noto Sans SC', monospace;
  font-size: clamp(12px, 1.8vw, 13.5px);
  line-height: 1.7;
  background:
    radial-gradient(ellipse 80% 50% at 8% 0%, rgba(31, 111, 139, 0.1), transparent 55%),
    radial-gradient(ellipse 70% 45% at 92% 100%, rgba(196, 92, 106, 0.08), transparent 50%),
    linear-gradient(180deg, rgba(244, 248, 252, 0.55), rgba(240, 245, 249, 0.72));
}

.terminal-body::before {
  content: '';
  pointer-events: none;
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(31, 111, 139, 0.04) 50%,
    transparent 100%
  );
  background-size: 100% 220%;
  animation: scan-line 3.6s linear infinite;
  opacity: 0.7;
}

.line {
  position: relative;
  margin: 0 0 8px;
  word-break: break-word;
}

.line:last-child {
  margin-bottom: 0;
}

.prompt {
  color: var(--color-primary);
  margin-right: 8px;
  font-weight: 600;
}

.cmd-text {
  color: var(--color-ink);
}

.line.info .msg-text {
  color: var(--color-text-secondary);
}

.line.success .msg-text {
  color: var(--color-primary);
  font-weight: 600;
  animation: success-glow 1.6s ease-in-out infinite;
}

.cursor {
  display: inline-block;
  margin-left: 2px;
  color: var(--color-primary);
  animation: blink 1s step-end infinite;
}

.line.idle {
  opacity: 0.85;
}

@keyframes border-flow {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 200% 50%;
  }
}

@keyframes shell-float {
  0%,
  100% {
    transform: translateY(0);
    box-shadow:
      0 16px 40px rgba(7, 21, 37, 0.14),
      0 0 24px rgba(31, 111, 139, 0.14);
  }
  50% {
    transform: translateY(-6px);
    box-shadow:
      0 22px 48px rgba(7, 21, 37, 0.18),
      0 0 36px rgba(196, 92, 106, 0.16);
  }
}

@keyframes panel-pulse {
  0%,
  100% {
    background: linear-gradient(160deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.58));
  }
  50% {
    background: linear-gradient(160deg, rgba(255, 255, 255, 0.92), rgba(244, 249, 252, 0.7));
  }
}

@keyframes shine-sweep {
  0%,
  35% {
    transform: translateX(-120%);
    opacity: 0;
  }
  45% {
    opacity: 0.8;
  }
  60% {
    transform: translateX(120%);
    opacity: 0;
  }
  100% {
    transform: translateX(120%);
    opacity: 0;
  }
}

@keyframes scan-line {
  0% {
    background-position: 0% -40%;
  }
  100% {
    background-position: 0% 140%;
  }
}

@keyframes dot-breathe {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.75;
  }
  50% {
    transform: scale(1.18);
    opacity: 1;
  }
}

@keyframes status-blink {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}

@keyframes success-glow {
  0%,
  100% {
    text-shadow: none;
  }
  50% {
    text-shadow: 0 0 12px rgba(31, 111, 139, 0.35);
  }
}

@media (max-width: 480px) {
  .boot-shell {
    margin-bottom: 24px;
  }

  .terminal-chrome {
    height: 36px;
  }

  .terminal-body {
    min-height: 148px;
    padding: 14px 14px 16px;
  }

  .traffic {
    left: 12px;
    gap: 6px;
  }

  .dot {
    width: 9px;
    height: 9px;
  }

  .status-pill {
    right: 10px;
    font-size: 9px;
    padding: 2px 6px;
  }
}

@media (max-height: 560px) and (orientation: landscape) {
  .boot-shell {
    margin-bottom: 16px;
  }

  .terminal-body {
    min-height: 110px;
    max-height: 140px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .boot-shell,
  .boot-terminal,
  .shine,
  .terminal-body::before,
  .dot,
  .status-pill,
  .line.success .msg-text {
    animation: none !important;
  }
}
</style>
