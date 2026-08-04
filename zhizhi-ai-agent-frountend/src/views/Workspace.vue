<template>
  <ChatRoom
    title="Agent Workspace"
    api-url="/api/zhizhi-ai/doChatByZhizhiManus"
    stop-api-url="/api/zhizhi-ai/stopChatByZhizhiManus"
    stop-type="COMMON"
    agent-type="SUPER_AGENT"
    :avatar="APP_AVATARS.workspace"
    empty-tip="Workspace：左侧历史 · 中间对话与思考 · 右侧计划/产物。可演示 HITL 审批与 Trace。"
    placeholder="描述任务，例如：写一个 hello.txt / 执行 ls / 搜索图片…"
    accent="agent"
    :step-mode="true"
    :workspace-mode="true"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { APP_AVATARS } from '../constants/apps.js'
</script>
