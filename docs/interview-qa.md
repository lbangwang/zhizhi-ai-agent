# 模拟面试自问自答

## 1. 这个项目解决什么问题？

做一个可演示的「企业味」Agent：不只是聊天，还要有登录隔离、会话历史、工具治理、可取消、产物下载和 Trace，方便面试讲清工程取舍。

## 2. ReAct 在你项目里怎么跑？

`ToolCallAgent`：`think` 让模型决定是否调工具 → `act` 用 Spring AI `ToolCallingManager` 执行 → 结果进会话消息 → 足够后综合用户可读回答。外层 `BaseAgent` 控制 maxSteps、停止信号与 SSE 事件。

## 3. 为什么 HITL 用阻塞 Future 而不是纯异步回调？

工具执行发生在模型工具调用线程上；在真正 `delegate.call` 前阻塞，能保证「未批准绝不执行」。前端用 SSE 收 `hitl_required`，用独立 HTTP approve/reject 完成 Future，职责清晰。

## 4. 用户点「停止」后模型 call 还在跑怎么办？

单次模型 HTTP 难以硬杀；我们保证 **不再进入下一个 step**（`ChatStopSignalService` + 循环前检查），并推送 `cancelled` /「已停止生成」。工程上这是常见可取消语义。

## 5. 停止信号为什么 Redis + 本地 Map？

多实例要共享用 Redis；本地开发或 Redis 宕机时 ConcurrentHashMap 兜底，单机仍可取消。

## 6. Planner-Worker 和单 Manus 有何区别？

Manus 是单 Agent 自规划自执行；多 Agent 把「拆步」和「执行」拆开：Planner 只产出 JSON 步骤，Worker 每步短 maxSteps 聚焦执行，便于讲解协作与限步，HITL/停止仍复用。

## 7. Trace / Token 怎么记？

任务开始生成 TraceId，SSE 推 `trace_meta`；结束时写状态、耗时、步数；模型调用处可累加 Token（视模型响应 metadata）。页面 `/trace` 做列表与汇总。

## 8. RAG 引用卡片怎么来的？

文档上传切片进 VectorStore；对话检索后后端在流式输出中带 `__CITATIONS__` 前缀，前端解析成卡片，避免用户看不到依据。

## 9. 工具审计和产物怎么做的？

工具批次结束后 `AgentToolObservabilityService` 写审计；识别 PDF/写文件/下载等结果路径，拷贝到产物目录并提供 REST 下载，Workspace 右侧「产物」面板展示。

## 10. 如果继续做一周，你优先加什么？

评测集回归、长任务中心（离开页面可回看）、以及更细的 Token 计量与限流；Workflow 可视化投入大、简历增益相对低，暂不做。

## 11. 安全上注意什么？

密钥只放 `.env`；接口鉴权；危险工具二次确认；终端/写文件默认拒绝无 HITL 上下文的调用；演示环境注意命令与路径沙箱边界。
