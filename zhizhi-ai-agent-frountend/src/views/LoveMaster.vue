<template>
  <ChatRoom
    title="AI面试官小助手CC"
    api-url="/api/zhizhi-ai/doChatBySynSSE"
    stop-api-url="/api/zhizhi-ai/stopChatByZhizhiManus"
    stop-type="PROFESSIONAL"
    agent-type="LOVE_MASTER"
    :avatar="APP_AVATARS.love"
    empty-tip="你好！我是专注于 AI 应用开发的 AI面试官小助手CC，求职方案、项目梳理、面试拆解都可以找我。"
    placeholder="说说你的求职目标或面试问题..."
    accent="love"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { APP_AVATARS } from '../constants/apps.js'
</script>
