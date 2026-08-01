<template>
  <div class="login-page">
    <ParticleBackground density="light" />
    <div class="login-card">
      <p class="brand">{{ brandName }}</p>
      <h1>{{ mode === 'login' ? '登录' : '注册' }}</h1>
      <p class="sub">登录后可同步历史会话，并保护你的对话数据</p>

      <form class="form" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <input v-model.trim="username" autocomplete="username" placeholder="3~32 位字母数字下划线" />
        </label>
        <label v-if="mode === 'register'">
          <span>昵称（可选）</span>
          <input v-model.trim="nickname" autocomplete="nickname" placeholder="显示名称" />
        </label>
        <label>
          <span>密码</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="至少 6 位"
          />
        </label>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? '提交中…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>

      <button class="switch" type="button" @click="toggleMode">
        {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
      </button>
      <button class="back" type="button" @click="$router.push('/')">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ParticleBackground from '../components/ParticleBackground.vue'
import { login, register } from '../api/auth.js'
import { BRAND_NAME } from '../constants/apps.js'

const brandName = BRAND_NAME
const route = useRoute()
const router = useRouter()

const mode = ref('login')
const username = ref('')
const nickname = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  error.value = ''
}

async function submit() {
  error.value = ''
  loading.value = true
  try {
    if (mode.value === 'login') {
      await login({ username: username.value, password: password.value })
    } else {
      await register({
        username: username.value,
        password: password.value,
        nickname: nickname.value || undefined,
      })
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect || '/')
  } catch (err) {
    error.value = err?.response?.data?.message || err.message || '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: calc(24px + var(--safe-top)) 16px calc(24px + var(--safe-bottom));
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 28px 24px 22px;
  border-radius: var(--radius-lg);
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.72));
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px);
}

.brand {
  margin: 0;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  letter-spacing: 0.04em;
}

h1 {
  margin: 8px 0 0;
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--color-ink);
}

.sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.form {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

label {
  display: grid;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

input {
  height: 42px;
  border: 1px solid rgba(15, 28, 46, 0.12);
  border-radius: 12px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-ink);
}

input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.error {
  margin: 0;
  font-size: 13px;
  color: #b42318;
}

.submit {
  height: 44px;
  border: none;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.switch,
.back {
  display: block;
  width: 100%;
  margin-top: 10px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 13px;
}

.switch:hover,
.back:hover {
  color: var(--color-primary);
}
</style>
