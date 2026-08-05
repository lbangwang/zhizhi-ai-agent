# 枝枝 AI 智能体 · 架构一页纸（面试用）

## 一句话

Vue3 Workspace 经 **JWT + SSE** 调用 Spring Boot Agent Runtime：单 Agent（ReAct + Tool Calling）与 **Planner→Worker** 多 Agent；危险工具 **HITL 阻塞确认**；停止靠 Redis/内存信号；会话/产物/审计/Trace 落 MySQL。

## 请求主链路

```text
浏览器 ChatRoom
  → GET /api/zhizhi-ai/doChatByZhizhiManus?message&chatId  (SSE)
  → ZhizhiManus.runStream
  → BaseAgent 循环：think → act（工具）→ synthesize
  → AgentStreamEvent：thinking_* / hitl_required / tool_done / answer_done
```

多 Agent：`/doChatByMultiAgent` → `MultiAgentOrchestrator`：Planner 拆步 JSON → 每步 Worker（短 maxSteps ToolCallAgent）→ 同一 `chatId` 共享停止与 HITL。

## 核心机制

| 机制 | 实现要点 |
|------|----------|
| ReAct | `ToolCallAgent.think()` 选工具；`act()` 执行；足够后综合用户可读回答 |
| HITL | `HitlGuardedToolCallback` 推 `hitl_required`，`CompletableFuture.get` 阻塞；前端 `POST /hitl/{id}/approve\|reject` |
| 停止 | 前端 abort + `POST/GET stopChatByZhizhiManus` → `ChatStopSignalService`；下一步循环前 `shouldStop` 退出 |
| Trace | `AgentTraceService` 记 TraceId / Token / 耗时 / 步数；页 `/trace` |
| RAG | 上传切片 → VectorStore；面试官流式带 `__CITATIONS__` 引用卡片 |
| MCP | 可选 `MCP_ENABLED`；本地/外部 image-search 工具合并进 `ToolCallback[]` |

## 分层（白板可画）

```text
UI（Workspace / MultiAgent / Knowledge / Trace）
        │ JWT + SSE
API（Controller + Sa-Token）
        │
Agent Runtime（BaseAgent / Manus / Planner-Worker）
   ├── Model Router（Qwen / DeepSeek / Doubao）
   ├── Tools + HITL 包装 + 审计
   └── Stop Signal
        │
MySQL（会话/消息/产物/审计/Trace）· Redis（停止）· VectorStore · 本地产物目录
```

## 面试追问锚点

1. **为何 HITL 用阻塞 Future？** 工具线程在 Spring AI 执行链上，阻塞可保证「未批准绝不执行」；审批走独立 HTTP，不占用 SSE 写方向歧义。  
2. **停止为何不能 kill 模型 call？** 单次模型 HTTP 难以硬中断；工程上保证「不再进入下一步」即可演示可取消。  
3. **Planner-Worker vs 单 ReAct？** Planner 显式拆步便于讲解与限步；Worker 每步聚焦，降低一次超长工具链跑偏概率。
