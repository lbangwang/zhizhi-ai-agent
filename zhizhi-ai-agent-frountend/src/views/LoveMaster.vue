<template>
  <ChatRoom
    title="CC面试官"
    api-url="/api/zhizhi-ai/doChatBySynSSE"
    stop-api-url="/api/zhizhi-ai/stopChatByZhizhiManus"
    stop-type="PROFESSIONAL"
    :chat-id="chatId"
    :avatar="APP_AVATARS.love"
    empty-tip="你好！我是专注于 AI 应用开发的 CC面试官，求职方案、项目梳理、面试拆解都可以找我。"
    placeholder="说说你的求职目标或面试问题..."
    accent="love"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import { generateChatId } from '../utils/chatId.js'
import { APP_AVATARS } from '../constants/apps.js'

const chatId = ref('')

onMounted(() => {
  chatId.value = generateChatId()
})
</script>
