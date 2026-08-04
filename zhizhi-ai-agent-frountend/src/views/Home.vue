<template>
  <div class="home">
    <ParticleBackground density="light" />

    <header class="topnav">
      <button type="button" class="brand-lockup" @click="$router.push('/')">
        <span class="brand-mark" aria-hidden="true" />
        <span class="brand-name">{{ brandName }}</span>
      </button>

      <nav class="nav-links" aria-label="主导航">
        <button type="button" class="nav-link" @click="goAuth('/workspace')">工作台</button>
        <button type="button" class="nav-link" @click="goAuth('/multi-agent')">多 Agent</button>
        <button type="button" class="nav-link" @click="goAuth('/knowledge')">知识库</button>
        <button type="button" class="nav-link" @click="goAuth('/trace')">Trace</button>
      </nav>

      <div class="nav-actions">
        <template v-if="user">
          <span class="nav-user">{{ user.nickname || user.username }}</span>
          <button type="button" class="btn ghost" @click="onLogout">退出</button>
        </template>
        <button v-else type="button" class="btn primary" @click="$router.push('/login')">
          登录
        </button>
      </div>
    </header>

    <main class="main">
      <!-- Hero：一屏一构图 -->
      <section class="hero">
        <div class="hero-copy">
          <p class="hero-brand">{{ brandName }}</p>
          <h1 class="hero-title">企业级 Agent 工作台</h1>
          <p class="hero-desc">
            工具调用、人机确认、知识库引用与任务 Trace，一站式演示可落地的智能体能力。
          </p>
          <div class="hero-cta">
            <button type="button" class="btn primary lg" @click="goAuth('/workspace')">
              进入工作台
            </button>
            <button
              type="button"
              class="btn ghost lg"
              @click="goAuth(user ? '/love-master' : '/login')"
            >
              {{ user ? '面试官助手' : '登录后体验' }}
            </button>
          </div>
        </div>

        <div class="hero-panel" aria-hidden="true">
          <div class="hero-panel-inner">
            <div class="panel-row">
              <span class="dot" />
              <span class="panel-label">Agent Workspace</span>
            </div>
            <div class="panel-lines">
              <span class="line w80" />
              <span class="line w60" />
              <span class="line w90" />
              <span class="line w45" />
            </div>
            <div class="panel-chips">
              <span>思考</span>
              <span>工具</span>
              <span>产物</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 产品入口：主推 + 其它 -->
      <section class="section" aria-labelledby="products-title">
        <div class="section-head">
          <h2 id="products-title">选择能力</h2>
          <p>从主工作台开始，或按场景进入对应应用。</p>
        </div>

        <button type="button" class="featured" @click="goAuth('/workspace')">
          <img class="featured-avatar" :src="avatars.workspace" alt="" />
          <div class="featured-body">
            <span class="featured-kicker">推荐入口</span>
            <h3>Agent Workspace</h3>
            <p>三栏工作台：历史 · 对话与思考 · 计划/产物。支持 HITL 审批与 Trace。</p>
          </div>
          <span class="featured-go">进入 <i>→</i></span>
        </button>

        <div class="product-grid">
          <button type="button" class="product" @click="goAuth('/love-master')">
            <img class="product-avatar" :src="avatars.love" alt="" />
            <h3>面试官小助手 CC</h3>
            <p>求职辅导与技术方案梳理，支持知识库引用卡片。</p>
          </button>

          <button type="button" class="product" @click="goAuth('/multi-agent')">
            <img class="product-avatar" :src="avatars.multiAgent" alt="" />
            <h3>多 Agent</h3>
            <p>Planner 拆步，Worker 逐步执行，适合讲多智能体协作。</p>
          </button>

          <button type="button" class="product" @click="goAuth('/knowledge')">
            <img class="product-avatar" :src="avatars.knowledge" alt="" />
            <h3>知识库</h3>
            <p>上传文档切片入库，对话时展示「来自哪篇文档」。</p>
          </button>

          <button type="button" class="product" @click="goAuth('/super-agent')">
            <img class="product-avatar" :src="avatars.agent" alt="" />
            <h3>超级智能体</h3>
            <p>与 Workspace 同一套 Manus，单栏聊天视图。</p>
          </button>
        </div>
      </section>

      <!-- 演示路径：轻量文字区 -->
      <section class="section demos" aria-labelledby="demos-title">
        <div class="section-head">
          <h2 id="demos-title">三分钟演示路径</h2>
          <p>按顺序点开即可走完核心卖点。</p>
        </div>
        <ol class="demo-steps">
          <li>
            <button type="button" class="demo-step" @click="goAuth('/workspace')">
              <strong>1 · HITL</strong>
              <span>Workspace 写 hello.txt，演示允许/拒绝</span>
            </button>
          </li>
          <li>
            <button type="button" class="demo-step" @click="goAuth('/knowledge')">
              <strong>2 · RAG</strong>
              <span>知识库上传后，用面试官对话看引用</span>
            </button>
          </li>
          <li>
            <button type="button" class="demo-step" @click="goAuth('/multi-agent')">
              <strong>3 · 多 Agent</strong>
              <span>看 Planner 步骤列表与 Worker 执行</span>
            </button>
          </li>
        </ol>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ParticleBackground from '../components/ParticleBackground.vue'
import SiteFooter from '../components/SiteFooter.vue'
import { logout } from '../api/auth.js'
import { APP_AVATARS, BRAND_NAME } from '../constants/apps.js'
import { getUser, isLoggedIn } from '../utils/auth.js'

const router = useRouter()
const brandName = BRAND_NAME
const avatars = APP_AVATARS
const user = ref(null)

onMounted(() => {
  user.value = isLoggedIn() ? getUser() : null
})

async function onLogout() {
  await logout()
  user.value = null
}

function goAuth(path) {
  if (!isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: path } })
    return
  }
  router.push(path)
}
</script>

<style scoped>
.home {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  padding:
    calc(16px + var(--safe-top))
    calc(var(--page-padding-x) + var(--safe-right))
    calc(8px + var(--safe-bottom))
    calc(var(--page-padding-x) + var(--safe-left));
}

.topnav,
.main,
:deep(.site-footer) {
  position: relative;
  z-index: 1;
}

/* —— Top nav —— */
.topnav {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  width: min(1120px, 100%);
  margin: 0 auto 8px;
  padding: 10px 4px;
  animation: page-enter 0.45s ease-out;
}

.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  justify-self: start;
  color: var(--color-ink);
}

.brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(145deg, #1f6f8b, #2f7a6b);
}

.brand-name {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.nav-link:hover {
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.45);
}

.nav-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.nav-user {
  font-size: 13px;
  color: var(--color-text-secondary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  min-height: 36px;
  padding: 0 14px;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease;
}

.btn.primary {
  background: var(--color-primary);
  color: #fff;
}

.btn.primary:hover {
  background: var(--color-primary-hover);
}

.btn.ghost {
  background: rgba(255, 255, 255, 0.55);
  border-color: rgba(18, 38, 58, 0.12);
  color: var(--color-text);
}

.btn.ghost:hover {
  border-color: rgba(31, 111, 139, 0.35);
  color: var(--color-primary);
}

.btn.lg {
  min-height: 44px;
  padding: 0 20px;
  font-size: 15px;
}

.btn:active {
  transform: scale(0.98);
}

/* —— Main —— */
.main {
  flex: 1;
  width: min(1120px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: clamp(28px, 4vw, 44px);
  padding-bottom: 24px;
}

/* —— Hero —— */
.hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: clamp(28px, 5vw, 56px);
  align-items: center;
  min-height: 0;
  padding: clamp(14px, 2.5vw, 24px) 0 clamp(10px, 1.5vw, 16px);
  animation: page-enter 0.55s ease-out;
}

.hero-brand {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: clamp(34px, 5.5vw, 52px);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 0.01em;
  background: linear-gradient(115deg, #0f1c2e 0%, #1f6f8b 42%, #2f7a6b 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-title {
  margin: 0 0 14px;
  font-family: var(--font-display);
  font-size: clamp(22px, 3vw, 30px);
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.25;
}

.hero-desc {
  margin: 0 0 28px;
  max-width: 36em;
  font-size: clamp(14px, 1.8vw, 16px);
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.hero-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-panel {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.65);
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.72), rgba(232, 242, 246, 0.55)),
    linear-gradient(120deg, rgba(31, 111, 139, 0.12), rgba(47, 122, 107, 0.1));
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  min-height: 280px;
  padding: 22px;
  animation: panel-rise 0.7s ease-out 0.1s both;
}

.hero-panel-inner {
  height: 100%;
  min-height: 236px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(31, 111, 139, 0.12);
  background: rgba(255, 255, 255, 0.55);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2f7a6b;
  animation: pulse-soft 2.4s ease-in-out infinite;
}

.panel-label {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.panel-lines {
  display: grid;
  gap: 10px;
  flex: 1;
}

.line {
  display: block;
  height: 10px;
  border-radius: 6px;
  background: linear-gradient(90deg, rgba(31, 111, 139, 0.18), rgba(31, 111, 139, 0.06));
}

.line.w80 { width: 80%; }
.line.w60 { width: 60%; }
.line.w90 { width: 90%; }
.line.w45 { width: 45%; }

.panel-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.panel-chips span {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  padding: 5px 10px;
  border-radius: var(--radius-sm);
}

@keyframes panel-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* —— Sections —— */
.section-head {
  margin-bottom: 20px;
}

.section-head h2 {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: clamp(20px, 2.6vw, 26px);
  font-weight: 650;
  color: var(--color-ink);
}

.section-head p {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-muted);
}

.featured {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 18px;
  align-items: center;
  text-align: left;
  padding: 22px 24px;
  margin-bottom: 16px;
  border: 1px solid rgba(31, 111, 139, 0.22);
  border-radius: var(--radius-lg);
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.82), rgba(232, 244, 248, 0.7));
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.featured:hover {
  border-color: rgba(31, 111, 139, 0.45);
  transform: translateY(-2px);
}

.featured-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  object-fit: cover;
}

.featured-kicker {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--color-primary);
}

.featured-body h3 {
  margin: 0 0 6px;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 650;
  color: var(--color-ink);
}

.featured-body p {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--color-text-secondary);
}

.featured-go {
  font-weight: 650;
  color: var(--color-primary);
  white-space: nowrap;
}

.featured-go i {
  font-style: normal;
  display: inline-block;
  transition: transform 0.2s ease;
}

.featured:hover .featured-go i {
  transform: translateX(3px);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.product {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  text-align: left;
  padding: 20px 18px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.58);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, background 0.2s ease;
}

.product:hover {
  border-color: rgba(31, 111, 139, 0.3);
  background: rgba(255, 255, 255, 0.82);
  transform: translateY(-2px);
}

.product-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: cover;
}

.product h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 650;
  color: var(--color-ink);
}

.product p {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text-secondary);
}

/* —— Demos —— */
.demo-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.demo-step {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  text-align: left;
  padding: 18px;
  border: 1px solid rgba(18, 38, 58, 0.08);
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.demo-step:hover {
  background: rgba(255, 255, 255, 0.5);
  border-color: rgba(31, 111, 139, 0.25);
}

.demo-step strong {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--color-ink);
}

.demo-step span {
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-muted);
}

@media (max-width: 960px) {
  .topnav {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'brand actions'
      'links links';
  }

  .brand-lockup { grid-area: brand; }
  .nav-actions { grid-area: actions; }
  .nav-links {
    grid-area: links;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .hero {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .hero-panel {
    min-height: 220px;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .demo-steps {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .featured {
    grid-template-columns: auto 1fr;
  }

  .featured-go {
    grid-column: 2;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }

  .nav-user {
    display: none;
  }
}
</style>
