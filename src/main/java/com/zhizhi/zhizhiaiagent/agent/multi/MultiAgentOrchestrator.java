package com.zhizhi.zhizhiaiagent.agent.multi;

import com.zhizhi.zhizhiaiagent.agent.hitl.HitlContext;
import com.zhizhi.zhizhiaiagent.agent.hitl.HitlDecisionTracker;
import com.zhizhi.zhizhiaiagent.agent.model.AgentStreamEvent;
import com.zhizhi.zhizhiaiagent.agent.model.AgentUserFacingFormatter;
import com.zhizhi.zhizhiaiagent.agent.observability.AgentToolObservabilityService;
import com.zhizhi.zhizhiaiagent.agent.stop.ChatStopSignalService;
import com.zhizhi.zhizhiaiagent.persistence.service.AgentTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Planner → 逐步 Worker 的最小多 Agent 编排（共用 chatId 停止信号与 HITL）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentOrchestrator {

    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final PlannerAgent plannerAgent;
    private final ChatStopSignalService chatStopSignalService;

    public SseEmitter runStream(
            String userMessage,
            String chatId,
            String userId,
            ChatModel chatModel,
            ChatOptions chatOptions,
            ToolCallback[] tools,
            AgentToolObservabilityService toolObservabilityService,
            AgentTraceService agentTraceService) {

        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT_MS);
        sseEmitter.onTimeout(() -> {
            try {
                sseEmitter.send(AgentStreamEvent.error("连接超时，请重试或缩短任务。"));
            } catch (Exception ignored) {
                // emitter may already be closed
            }
            sseEmitter.complete();
            log.warn("MultiAgent SSE timeout chatId={}", chatId);
        });
        sseEmitter.onCompletion(() -> log.info("MultiAgent SSE complete chatId={}", chatId));

        CompletableFuture.runAsync(() -> {
            try {
                execute(
                        userMessage,
                        chatId,
                        userId,
                        chatModel,
                        chatOptions,
                        tools,
                        toolObservabilityService,
                        agentTraceService,
                        sseEmitter);
            } catch (Exception e) {
                log.error("MultiAgent failed chatId={}", chatId, e);
                try {
                    sseEmitter.send(AgentStreamEvent.error("多 Agent 执行异常：" + e.getMessage()));
                    sseEmitter.complete();
                } catch (Exception ex) {
                    sseEmitter.completeWithError(ex);
                }
            }
        });
        return sseEmitter;
    }

    private void execute(
            String userMessage,
            String chatId,
            String userId,
            ChatModel chatModel,
            ChatOptions chatOptions,
            ToolCallback[] tools,
            AgentToolObservabilityService toolObservabilityService,
            AgentTraceService agentTraceService,
            SseEmitter sse) throws IOException {

        if (StringUtils.isBlank(userMessage)) {
            sse.send(AgentStreamEvent.error("用户提示词不能为空"));
            sse.complete();
            return;
        }

        String id = StringUtils.trimToEmpty(chatId);
        if (StringUtils.isNotBlank(id)) {
            chatStopSignalService.clear(id);
        }

        String traceId = null;
        if (!Objects.isNull(agentTraceService)) {
            try {
                traceId = agentTraceService.start(userId, id, "MULTI_AGENT");
                sse.send(AgentStreamEvent.traceMeta(traceId));
            } catch (Exception e) {
                log.warn("start multi-agent trace failed: {}", e.getMessage());
            }
        }

        long startedAt = System.currentTimeMillis();
        String runStatus = "SUCCESS";
        String errorMessage = null;
        List<String> stepSummaries = new ArrayList<>();

        try {
            sse.send(AgentStreamEvent.thinkingStart());
            sse.send(AgentStreamEvent.thinkingDelta(0, "【Planner】正在拆解任务…"));

            if (shouldStop(id)) {
                runStatus = "CANCELLED";
                finishCancelled(sse);
                return;
            }

            PlannerPlan plan = plannerAgent.plan(chatModel, userMessage);
            StringBuilder planText = new StringBuilder();
            planText.append("【Planner】目标：").append(plan.goal()).append("\n步骤：\n");
            for (int i = 0; i < plan.steps().size(); i++) {
                planText.append(i + 1).append(". ").append(plan.steps().get(i)).append("\n");
            }
            sse.send(AgentStreamEvent.thinkingDelta(0, planText.toString().trim()));

            int total = plan.steps().size();
            for (int i = 0; i < total; i++) {
                if (shouldStop(id)) {
                    runStatus = "CANCELLED";
                    finishCancelled(sse);
                    return;
                }

                String step = plan.steps().get(i);
                int stepNo = i + 1;
                sse.send(AgentStreamEvent.thinkingProgress(stepNo, total, "Worker 执行步骤 " + stepNo));
                sse.send(AgentStreamEvent.thinkingDelta(stepNo, "【Worker】开始： " + step));

                WorkerAgent worker = new WorkerAgent(tools, chatModel, chatOptions);
                worker.setChatId(id);
                worker.setUserId(userId);
                worker.setStopSignalService(chatStopSignalService);
                worker.setAgentType("MULTI_AGENT_WORKER");
                if (!Objects.isNull(toolObservabilityService)) {
                    worker.setToolObservabilityService(toolObservabilityService);
                }

                HitlDecisionTracker.clear();
                HitlContext.set(id, userId, event -> {
                    try {
                        sse.send(event);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                String rawResult;
                try {
                    rawResult = worker.run(
                            "总体目标：" + plan.goal()
                                    + "\n当前步骤（" + stepNo + "/" + total + "）：" + step
                                    + "\n请只完成本步骤。");
                } finally {
                    HitlContext.clear();
                    HitlDecisionTracker.clear();
                }

                List<String> names = worker.getLastToolNames();
                List<String> raws = worker.getLastToolRawResults();
                for (int t = 0; t < names.size(); t++) {
                    String toolName = names.get(t);
                    String raw = t < raws.size() ? raws.get(t) : null;
                    boolean rejected = AgentUserFacingFormatter.isHitlRejectedResult(raw);
                    String summary = rejected
                            ? AgentUserFacingFormatter.toolLabel(toolName) + " 已拒绝"
                            : AgentUserFacingFormatter.toolDoneSummary(toolName, raw);
                    String outcome = rejected || summary.endsWith("已拒绝") ? "rejected"
                            : (summary.endsWith("失败") || summary.endsWith("已超时") || summary.endsWith("已结束")
                            ? "failed" : "success");
                    sse.send(AgentStreamEvent.toolDone(stepNo, toolName, summary, outcome));
                }

                String display = StringUtils.isNotBlank(worker.getLastActDisplayText())
                        ? worker.getLastActDisplayText()
                        : (rawResult == null ? "" : rawResult);
                if (StringUtils.isNotBlank(display)
                        && !AgentUserFacingFormatter.looksLikeRawToolDump(display)) {
                    sse.send(AgentStreamEvent.thinkingDelta(stepNo, "【Worker】步骤 " + stepNo + " 结果：\n" + display));
                    stepSummaries.add(stepNo + ". " + step + " → " + truncate(display, 200));
                } else {
                    stepSummaries.add(stepNo + ". " + step + " → 已执行");
                }
            }

            String answer = buildFinalAnswer(plan, stepSummaries);
            long thinkMs = System.currentTimeMillis() - startedAt;
            sse.send(AgentStreamEvent.thinkingDone(null, thinkMs));
            sse.send(AgentStreamEvent.answerDone(answer));
            sse.complete();
        } catch (Exception e) {
            runStatus = "ERROR";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            if (StringUtils.isNotBlank(id)) {
                chatStopSignalService.clear(id);
            }
            if (!Objects.isNull(agentTraceService) && StringUtils.isNotBlank(traceId)) {
                try {
                    agentTraceService.finish(
                            traceId,
                            runStatus,
                            System.currentTimeMillis() - startedAt,
                            stepSummaries.size(),
                            errorMessage);
                } catch (Exception e) {
                    log.warn("finish multi-agent trace failed: {}", e.getMessage());
                }
            }
        }
    }

    private void finishCancelled(SseEmitter sse) throws IOException {
        sse.send(AgentStreamEvent.cancelled("已停止生成"));
        sse.send(AgentStreamEvent.answerDone("已停止生成"));
        sse.complete();
    }

    private boolean shouldStop(String chatId) {
        return StringUtils.isNotBlank(chatId) && chatStopSignalService.shouldStop(chatId);
    }

    private static String buildFinalAnswer(PlannerPlan plan, List<String> stepSummaries) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 多 Agent 执行结果\n\n");
        sb.append("**目标：** ").append(plan.goal()).append("\n\n");
        sb.append("**步骤摘要：**\n");
        for (String line : stepSummaries) {
            sb.append("- ").append(line).append("\n");
        }
        sb.append("\n以上由 Planner 拆解、Worker 逐步执行完成。");
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
