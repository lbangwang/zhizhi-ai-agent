<template>
  <ChatRoom
    title="AI 超级智能体"
    api-url="/api/zhizhi-ai/doChatByZhizhiManus"
    stop-api-url="/api/zhizhi-ai/stopChatByZhizhiManus"
    stop-type="COMMON"
    :chat-id="chatId"
    :avatar="APP_AVATARS.agent"
    empty-tip="你好！我是 AI 超级智能体，可以帮你搜索、分析并解决各类问题。"
    placeholder="描述你需要完成的任务..."
    accent="agent"
    :step-mode="true"
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
