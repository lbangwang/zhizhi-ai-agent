# Demo 口播脚本（3～5 分钟）

前置：后端 `./scripts/run-dev.sh`，前端 `npm run dev`（http://127.0.0.1:5173），已登录。

---

## 0:00–0:20 开场

> 这是枝枝 AI 智能体，基于 Spring Boot + Spring AI + Vue3。  
> 今天演示三点：工作台与人机确认、产物与可观测、以及 Planner→Worker 多 Agent。

打开首页，指一下卡片：Workspace / 知识库 / Trace / 多 Agent。

---

## 0:20–2:00 场景 A：HITL（Workspace）

1. 进入 **Agent Workspace**。  
2. 发送：

```text
帮忙写一个 hello.txt，内容为「早上好，枝枝」，只写 txt，不要 PDF
```

3. 右侧「计划」出现「等待确认：写入文件」，中间弹出确认框。  
4. 先点 **拒绝**：

> 危险写文件不会静默执行；拒绝后计划应显示「写入文件 已拒绝」，对话也会说明未写入。

5.（可选，时间够）再发一次同样问题，点 **允许**，切到「产物」展示可下载。

话术收尾：

> 后端在工具执行线程用 Future 阻塞等待审批，前端 SSE 收 `hitl_required`，HTTP 回传 approve/reject。

---

## 2:00–3:20 场景 B：产物 + Trace

1. 在 Workspace 发送（允许 HITL）：

```text
请生成一份简短的 hello.pdf，介绍枝枝 AI 智能体一句话即可
```

2. 右侧「产物」刷新，点下载。  
3. 打开 **/trace**：指 TraceId、Token、耗时。

> 单次任务可回答「花了多少 Token、跑了多少步」，方便面试讲可观测。

---

## 3:20–4:20 场景 C：多 Agent（可选压缩）

1. 回首页进 **多 Agent（Planner→Worker）**。  
2. 发送：

```text
帮我规划并完成：1）用一句话介绍项目 2）写入 intro.txt
```

3. 思考区先出现 Planner 步骤列表，再逐步 Worker 执行；写文件时再次演示 HITL。

> Planner 只负责拆步；Worker 短步数带工具执行，停止信号与 HITL 与单 Agent 共用。

---

## 4:20–5:00 收尾卖点（15～20 秒）

逐条点出（不必展开）：

- 企业味：登录隔离、会话历史、可取消、工具审计  
- 安全：危险工具二次确认  
- 交付：产物下载 + Trace  
- 扩展：MCP 图片搜索、Planner-Worker  

> 完整架构与简历条目在仓库 `docs/` 里，欢迎提问。

---

## 录制注意

- 分辨率 1080p；字体放大；勿暴露 `.env` / 真实 Key。  
- HITL 弹窗务必完整入镜。  
- 若模型较慢，口播可先讲结构再等结果。
