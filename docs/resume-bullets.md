# 简历条目（可直接改写投递）

> 按岗位改动词与量化数字；下面每条对应仓库真实能力。

## 项目名称建议

**枝枝 AI 智能体**｜Spring Boot + Spring AI + Vue3 企业级 Agent 平台（个人项目）

## Bullet 示例（选 4～6 条）

1. 基于 **Spring AI Tool Calling** 实现 ReAct 超级智能体，支持网页搜索/爬取、文件读写、终端、PDF 生成等工具链，前端以 **SSE 结构化事件**（思考 / 工具 / 回答）实时展示推理过程。  

2. 设计危险工具 **Human-in-the-Loop**：写文件/终端执行前 SSE 推送确认，后端 `CompletableFuture` 阻塞等待，审批通过后才执行，拒绝/超时可区分计划状态，避免静默副作用。  

3. 落地 **会话持久化与鉴权**：Sa-Token JWT、MySQL 会话/消息隔离；Redis（及本地兜底）停止信号，支持前端中断后 Agent **不再继续 step**。  

4. 构建 **RAG 知识库**链路（上传 → 切片 → 向量检索），面试官对话展示「来自哪篇文档」的引用卡片，提升回答可追溯性。  

5. 实现产物与可观测：工具审计表、产物入库下载；单次任务 **TraceId + Token/耗时/步数** 落库，并提供 Trace 统计页便于演示成本与排障。  

6. 扩展 **MCP** 图片搜索工具（可开关）与 **Planner→Worker** 多 Agent 最小链路：Planner 拆步、Worker 短步执行，复用 HITL/停止/Trace，适合讲解多智能体协作。  

## 一句话项目介绍（简历摘要栏）

面向求职演示的企业级 Agent 平台：多模型路由、ReAct 工具调用、RAG、HITL 审批、可取消任务、产物交付与 Trace 可观测，含 Planner-Worker 多 Agent 演示。
