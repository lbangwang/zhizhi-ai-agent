<template>
  <ChatRoom
    title="AI 恋爱大师"
    api-url="/api/zhizhi-ai/doChatBySynSSE"
    stop-api-url="/api/zhizhi-ai/stopChatByZhizhiManus"
    stop-type="PROFESSIONAL"
    :chat-id="chatId"
    :avatar="APP_AVATARS.love"
    empty-tip="你好！我是你的 AI 恋爱大师，有什么情感问题想聊聊吗？"
    placeholder="说说你的情感困惑..."
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
