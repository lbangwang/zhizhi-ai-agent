<template>
  <ChatRoom
    title="多 Agent（Planner → Worker）"
    api-url="/api/zhizhi-ai/doChatByMultiAgent"
    stop-api-url="/api/zhizhi-ai/stopChatByZhizhiManus"
    stop-type="COMMON"
    agent-type="MULTI_AGENT"
    :avatar="APP_AVATARS.multiAgent"
    empty-tip="Planner 先拆步骤，Worker 逐步执行。写文件/终端会触发 HITL；停止与 Workspace 共用。"
    placeholder="例如：规划并完成：写 intro.txt，内容为一句话项目介绍"
    accent="agent"
    :step-mode="true"
    :workspace-mode="true"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { APP_AVATARS } from '../constants/apps.js'
</script>
