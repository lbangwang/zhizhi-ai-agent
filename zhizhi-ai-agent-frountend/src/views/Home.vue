<template>
  <div class="home">
    <ParticleBackground density="dense" />
    <div class="orb orb-a" aria-hidden="true" />
    <div class="orb orb-b" aria-hidden="true" />
    <div class="orb orb-c" aria-hidden="true" />

    <div class="home-main">
      <div class="home-content">
        <div class="auth-bar">
          <template v-if="user">
            <span class="auth-user">{{ user.nickname || user.username }}</span>
            <button class="auth-btn" type="button" @click="onLogout">退出</button>
          </template>
          <button v-else class="auth-btn primary" type="button" @click="$router.push('/login')">
            登录 / 注册
          </button>
        </div>
        <div class="brand-block">
          <span class="brand-badge">AI Agents Hub</span>
          <p class="brand">{{ brandName }}</p>
          <h1 class="title">探索智能对话的无限可能</h1>
          <p class="subtitle">选择一个应用，开启沉浸式 AI 体验</p>
        </div>

        <BootTerminal />

        <div class="app-grid">
          <button class="app-card love" type="button" @click="$router.push('/love-master')">
            <div class="card-glow" aria-hidden="true" />
            <img class="app-avatar" :src="avatars.love" alt="AI面试官小助手CC头像" />
            <div class="app-body">
              <h2>AI面试官小助手CC</h2>
              <p>深耕 AI 应用开发，助力技术方案、项目梳理与面试攻坚</p>
              <span class="enter-link">立即体验 <i>→</i></span>
            </div>
          </button>

          <button class="app-card agent" type="button" @click="$router.push('/super-agent')">
            <div class="card-glow" aria-hidden="true" />
            <img class="app-avatar" :src="avatars.agent" alt="AI 超级智能体头像" />
            <div class="app-body">
              <h2>AI 超级智能体</h2>
              <p>具备工具调用能力，分步拆解并完成复杂任务</p>
              <span class="enter-link">立即体验 <i>→</i></span>
            </div>
          </button>
        </div>
      </div>
    </div>
    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ParticleBackground from '../components/ParticleBackground.vue'
import BootTerminal from '../components/BootTerminal.vue'
import SiteFooter from '../components/SiteFooter.vue'
import { logout } from '../api/auth.js'
import { APP_AVATARS, BRAND_NAME } from '../constants/apps.js'
import { getUser, isLoggedIn } from '../utils/auth.js'

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
</script>

<style scoped>
.home {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding:
    calc(var(--page-padding-y) + var(--safe-top))
    calc(var(--page-padding-x) + var(--safe-right))
    calc(8px + var(--safe-bottom))
    calc(var(--page-padding-x) + var(--safe-left));
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  pointer-events: none;
  z-index: 0;
  animation: float-orb 12s ease-in-out infinite;
}

.orb-a {
  width: min(420px, 70vw);
  height: min(420px, 70vw);
  top: -8%;
  left: -6%;
  background: rgba(31, 111, 139, 0.28);
}

.orb-b {
  width: min(360px, 60vw);
  height: min(360px, 60vw);
  right: -8%;
  bottom: 8%;
  background: rgba(47, 122, 107, 0.22);
  animation-delay: -4s;
}

.orb-c {
  width: min(240px, 45vw);
  height: min(240px, 45vw);
  top: 42%;
  left: 48%;
  background: rgba(95, 168, 192, 0.18);
  animation-delay: -7s;
  animation-duration: 15s;
}

.home-main {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.home-content {
  width: 100%;
  max-width: 900px;
  text-align: center;
  animation: page-enter 0.55s ease-out;
}

.auth-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.auth-user {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.auth-btn {
  min-height: 34px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.7);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 13px;
}

.auth-btn.primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.brand-block {
  margin-bottom: clamp(20px, 3.5vw, 28px);
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 14px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-primary);
  box-shadow: var(--shadow-soft);
  animation: pulse-soft 3.6s ease-in-out infinite;
}

.brand {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: clamp(36px, 8vw, 60px);
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.12;
  background: linear-gradient(
    110deg,
    #0f1c2e 0%,
    #1f6f8b 28%,
    #2f7a6b 55%,
    #1f6f8b 78%,
    #0f1c2e 100%
  );
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: shimmer 6s linear infinite;
}

.title {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: clamp(18px, 3.2vw, 26px);
  font-weight: 600;
  color: var(--color-text-secondary);
}

.subtitle {
  margin: 0;
  font-size: clamp(13px, 2.2vw, 15px);
  color: var(--color-text-muted);
  line-height: 1.6;
  padding: 0 8px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(14px, 2.5vw, 22px);
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
  min-height: 100%;
  padding: clamp(22px, 3vw, 30px) clamp(18px, 2.5vw, 26px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.5));
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--shadow-card);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease,
    border-color 0.3s ease;
}

.card-glow {
  position: absolute;
  inset: auto -20% -40% -20%;
  height: 55%;
  opacity: 0;
  filter: blur(28px);
  transition: opacity 0.35s ease;
  pointer-events: none;
}

.app-card.love .card-glow {
  background: rgba(47, 122, 107, 0.45);
}

.app-card.agent .card-glow {
  background: rgba(31, 111, 139, 0.45);
}

@media (hover: hover) and (pointer: fine) {
  .app-card:hover {
    transform: translateY(-8px) scale(1.015);
    box-shadow: var(--shadow-glow), 0 18px 48px rgba(7, 21, 37, 0.16);
  }

  .app-card.love:hover {
    border-color: rgba(47, 122, 107, 0.45);
  }

  .app-card.agent:hover {
    border-color: rgba(31, 111, 139, 0.45);
  }

  .app-card:hover .card-glow {
    opacity: 1;
  }

  .app-card:hover .enter-link i {
    transform: translateX(4px);
  }

  .app-card:hover .app-avatar {
    transform: scale(1.06) rotate(-2deg);
  }
}

.app-card:active {
  transform: scale(0.985);
}

.app-avatar {
  position: relative;
  z-index: 1;
  width: 60px;
  height: 60px;
  border-radius: 18px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 0 8px 20px rgba(7, 21, 37, 0.12);
  transition: transform 0.3s ease;
}

.app-body {
  position: relative;
  z-index: 1;
  width: 100%;
}

.app-body h2 {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: clamp(17px, 2.4vw, 21px);
  font-weight: 600;
  color: var(--color-ink);
}

.app-body p {
  margin: 0 0 18px;
  font-size: clamp(13px, 1.8vw, 14px);
  line-height: 1.65;
  color: var(--color-text-secondary);
  min-height: 46px;
}

.enter-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.enter-link i {
  font-style: normal;
  transition: transform 0.25s ease;
}

.app-card.love .enter-link {
  color: var(--color-accent-love);
}

.app-card.agent .enter-link {
  color: var(--color-accent-agent);
}

:deep(.site-footer) {
  position: relative;
  z-index: 1;
}

@media (max-width: 900px) {
  .home-content {
    max-width: 720px;
  }

  .app-body p {
    min-height: 0;
  }
}

@media (max-width: 720px) {
  .home {
    padding-top: calc(48px + var(--safe-top));
  }

  .home-main {
    align-items: flex-start;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }

  .app-card {
    flex-direction: row;
    align-items: flex-start;
    gap: 14px;
  }

  .app-avatar {
    width: 52px;
    height: 52px;
    border-radius: 14px;
  }

  .app-body p {
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .home {
    padding-top: calc(36px + var(--safe-top));
  }

  .app-card {
    padding: 18px 16px;
  }

  .brand-badge {
    font-size: 11px;
    padding: 5px 12px;
  }
}

@media (max-height: 560px) and (orientation: landscape) {
  .home {
    padding-top: calc(16px + var(--safe-top));
    padding-bottom: calc(8px + var(--safe-bottom));
  }

  .home-main {
    align-items: flex-start;
  }

  .brand-block {
    margin-bottom: 20px;
  }

  .app-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .app-card {
    flex-direction: column;
  }
}
</style>
