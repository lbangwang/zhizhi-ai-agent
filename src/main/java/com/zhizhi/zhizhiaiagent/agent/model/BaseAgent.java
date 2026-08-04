package com.zhizhi.zhizhiaiagent.agent.model;

import com.zhizhi.zhizhiaiagent.agent.hitl.HitlContext;
import com.zhizhi.zhizhiaiagent.agent.hitl.HitlDecisionTracker;
import com.zhizhi.zhizhiaiagent.agent.model.enums.AgentStatus;
import com.zhizhi.zhizhiaiagent.agent.model.exception.BusinessException;
import com.zhizhi.zhizhiaiagent.agent.observability.AgentToolObservabilityService;
import com.zhizhi.zhizhiaiagent.agent.stop.ChatStopSignalService;
import com.zhizhi.zhizhiaiagent.persistence.service.AgentTraceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 基类：维护运行状态与会话消息，提供同步执行与 SSE 流式执行入口。
 */
@Slf4j
@Data
public abstract class BaseAgent {

    private static final long SSE_TIMEOUT_MS = 300_000L;
    private static final String FALLBACK_EMPTY_ANSWER = "抱歉，本次未能生成有效回答，请换个问法再试。";
    private static final String LEGACY_MAX_STEP_HINT = "已达到最大推理步骤，以下为当前结论。";
    private static final String STOPPED_ANSWER = "已停止生成";

    /** Agent 名称 */
    private String name;
    /** 运行状态 */
    private AgentStatus status = AgentStatus.IDLE;
    /** 系统提示词 */
    private String systemPrompt;
    /** 每轮下一步行动提示 */
    private String nextStepPrompt;
    /** 最大推理步数 */
    private int maxSteps = 10;
    /** 当前步序号 */
    private int currentStep = 1;
    /** 对话客户端 */
    private ChatClient chatClient;
    /** 本轮会话消息 */
    private List<Message> messageList = new ArrayList<>();
    /** 业务会话 ID（用于停止信号） */
    private String chatId;
    /** 当前登录用户 ID（审计 / 产物） */
    private String userId;
    /** 停止信号服务（由 Controller 注入） */
    private ChatStopSignalService stopSignalService;
    /** 工具审计 + 产物入库（由 Controller 注入，MySQL 开启时可用） */
    private AgentToolObservabilityService toolObservabilityService;
    /** Trace 落库（由 Controller 注入，MySQL 开启时可用） */
    private AgentTraceService agentTraceService;
    /** 本轮任务 TraceId */
    private String traceId;
    /** 智能体类型标签（SUPER_AGENT 等） */
    private String agentType = "SUPER_AGENT";
    /** 累计 prompt/completion token（由 think 等调用累加） */
    private int accumulatedPromptTokens;
    private int accumulatedCompletionTokens;

    /**
     * 同步执行 Agent：校验入参后循环调用 {@link #step()}，直到完成、取消或达到最大步数。
     *
     * @param userPrompt 用户输入
     * @return 各步骤结果拼接文本
     */
    public String run(String userPrompt) {
        if (this.status != AgentStatus.IDLE) {
            throw new BusinessException("模型正在运行中，请勿重复运行", "MODEL_RUNNING");
        }
        if (StringUtils.isBlank(userPrompt)) {
            throw new BusinessException("用户提示词不能为空", "USER_PROMPT_EMPTY");
        }

        this.status = AgentStatus.RUNNING;
        this.messageList.add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        try {
            this.currentStep = 1;
            while (this.currentStep <= this.maxSteps && canContinueLoop()) {
                if (isStopRequested()) {
                    markCancelled();
                    results.add("step " + this.currentStep + ": " + STOPPED_ANSWER);
                    break;
                }
                log.info("当前步骤：{}/{}", this.currentStep, this.maxSteps);
                results.add("step " + this.currentStep + ": " + step());
                this.currentStep++;
            }
            if (this.status == AgentStatus.CANCELLED) {
                return String.join("\n", results);
            }
            if (currentStep > maxSteps) {
                this.status = AgentStatus.FINISHED;
                results.add("模型执行完成，已超过最大步骤限制");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            this.status = AgentStatus.ERROR;
            log.error("模型运行异常", e);
            throw new BusinessException("模型运行异常", "MODEL_RUN_ERROR");
        } finally {
            this.cleanup();
        }
    }

    /**
     * SSE 流式执行入口：创建连接、绑定超时/完成回调，并异步启动流式对话编排。
     *
     * @param userPrompt 用户输入
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT_MS);
        sseEmitter.onTimeout(() -> {
            status = AgentStatus.ERROR;
            this.cleanup();
            log.warn("SSE连接超时");
        });
        sseEmitter.onCompletion(() -> {
            if (status == AgentStatus.RUNNING) {
                status = AgentStatus.FINISHED;
            }
            this.cleanup();
            log.info("SSE连接完成");
        });

        CompletableFuture.runAsync(() -> {
            try {
                executeAgentStream(userPrompt, sseEmitter);
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        });
        return sseEmitter;
    }

    /**
     * 流式流程：校验状态 → 逐步 think/act → 兜底综合结论 → 推送思考完成与最终回答。
     * 事件顺序：thinking_start → thinking_delta/tool_done → thinking_done → answer_done。
     * 每步开始前轮询停止信号；若已停止则不再继续 step。
     *
     * @param userPrompt 用户输入
     * @param sseEmitter SSE 输出通道
     */
    private void executeAgentStream(String userPrompt, SseEmitter sseEmitter) throws IOException {
        if (this.status != AgentStatus.IDLE) {
            sseEmitter.send(AgentStreamEvent.error("模型正在运行中，请勿重复运行"));
            sseEmitter.complete();
            return;
        }
        if (StringUtils.isBlank(userPrompt)) {
            sseEmitter.send(AgentStreamEvent.error("用户提示词不能为空"));
            sseEmitter.complete();
            return;
        }

        this.status = AgentStatus.RUNNING;
        this.messageList.add(new UserMessage(userPrompt));
        this.accumulatedPromptTokens = 0;
        this.accumulatedCompletionTokens = 0;

        String runStatus = "SUCCESS";
        String errorMessage = null;
        long thinkingStartedAt = System.currentTimeMillis();
        try {
            //初始化链路追踪信息，并落入数据库
            if (!Objects.isNull(agentTraceService)) {
                try {
                    this.traceId = agentTraceService.start(userId, chatId, agentType);
                    sseEmitter.send(AgentStreamEvent.traceMeta(this.traceId));
                } catch (Exception e) {
                    log.warn("start trace failed: {}", e.getMessage());
                }
            }

            sseEmitter.send(AgentStreamEvent.thinkingStart());

            String finalAnswer = null;
            this.currentStep = 1;
            while (this.currentStep <= this.maxSteps && canContinueLoop()) {
                //校验用户是否已经停止
                if (isStopRequested()) {
                    markCancelled();
                    break;
                }
                log.info("当前步骤：{}/{}", this.currentStep, this.maxSteps);
                boolean lastStep = this.currentStep >= this.maxSteps;
                String phase = lastStep ? "正在整理最终结论…" : "正在分析问题并规划行动…";
                sseEmitter.send(AgentStreamEvent.thinkingProgress(this.currentStep, this.maxSteps, phase));

                if (this instanceof ReActAgent reActAgent) {
                    String stepAnswer = runOneReActStep(reActAgent, lastStep, sseEmitter);
                    if (this.status == AgentStatus.CANCELLED) {
                        break;
                    }
                    if (stepAnswer != null || this.status == AgentStatus.FINISHED) {
                        finalAnswer = stepAnswer;
                        break;
                    }
                } else {
                    String stepResult = step();
                    if (StringUtils.isNotBlank(stepResult)
                            && !AgentUserFacingFormatter.looksLikeRawToolDump(stepResult)) {
                        sseEmitter.send(AgentStreamEvent.thinkingDelta(this.currentStep, stepResult.trim()));
                    }
                }
                this.currentStep++;
            }

            long elapsedMs = System.currentTimeMillis() - thinkingStartedAt;

            if (this.status == AgentStatus.CANCELLED) {
                runStatus = "CANCELLED";
                sseEmitter.send(AgentStreamEvent.cancelled(STOPPED_ANSWER));
                sseEmitter.send(AgentStreamEvent.thinkingDone(null, elapsedMs));
                sseEmitter.send(AgentStreamEvent.answerDone(STOPPED_ANSWER));
                sseEmitter.complete();
                return;
            }

            if (this.status != AgentStatus.FINISHED) {
                this.status = AgentStatus.FINISHED;
            }

            // 兜底：无有效结论时强制综合成用户可读回答
            if (StringUtils.isBlank(finalAnswer)
                    || AgentUserFacingFormatter.looksLikeRawToolDump(finalAnswer)
                    || LEGACY_MAX_STEP_HINT.equals(finalAnswer)) {
                if (this instanceof ToolCallAgent toolCallAgent) {
                    sseEmitter.send(AgentStreamEvent.thinkingDelta(
                            this.currentStep, "正在综合工具结果并生成最终回答…"));
                    finalAnswer = toolCallAgent.synthesizeUserFacingAnswer();
                } else if (this instanceof ReActAgent reActAgent) {
                    finalAnswer = AgentUserFacingFormatter.toAnswerDisplay(reActAgent.getFinalAnswer());
                }
            }

            sseEmitter.send(AgentStreamEvent.thinkingDone(null, elapsedMs));
            if (StringUtils.isBlank(finalAnswer)) {
                finalAnswer = FALLBACK_EMPTY_ANSWER;
            }
            sseEmitter.send(AgentStreamEvent.answerDone(AgentUserFacingFormatter.toAnswerDisplay(finalAnswer)));
            sseEmitter.complete();
        } catch (Exception e) {
            this.status = AgentStatus.ERROR;
            runStatus = "ERROR";
            errorMessage = e.getMessage();
            log.error("模型运行异常", e);
            sseEmitter.send(AgentStreamEvent.error("模型运行异常：" + e.getMessage()));
            sseEmitter.complete();
        } finally {
            finishTrace(runStatus, System.currentTimeMillis() - thinkingStartedAt, errorMessage);
            this.cleanup();
        }
    }

    private void finishTrace(String status, long durationMs, String errorMessage) {
        if (agentTraceService == null || StringUtils.isBlank(traceId)) {
            return;
        }
        try {
            if (accumulatedPromptTokens > 0 || accumulatedCompletionTokens > 0) {
                agentTraceService.addTokens(traceId, accumulatedPromptTokens, accumulatedCompletionTokens);
            }
            agentTraceService.finish(traceId, status, durationMs, Math.max(0, currentStep), errorMessage);
        } catch (Exception e) {
            log.warn("finish trace failed: {}", e.getMessage());
        }
    }

    /** 累加模型 Usage（由 think/synthesize 调用）。 */
    public void accumulateUsage(Integer promptTokens, Integer completionTokens) {
        if (promptTokens != null) {
            this.accumulatedPromptTokens += promptTokens;
        }
        if (completionTokens != null) {
            this.accumulatedCompletionTokens += completionTokens;
        }
    }

    /**
     * 执行一轮 ReAct：推送思考文案；若需行动则调用工具并推送摘要；必要时综合最终回答。
     * think 与 act 之间也会检查停止信号，避免取消后仍执行工具。
     *
     * @param reActAgent 当前 ReAct 智能体
     * @param lastStep   是否已到最大步
     * @param sseEmitter SSE 输出通道
     * @return 本步已得到最终回答时返回文案；否则返回 null 表示继续下一步
     */
    private String runOneReActStep(ReActAgent reActAgent, boolean lastStep, SseEmitter sseEmitter)
            throws IOException {
        reActAgent.setLastStepFinalAnswer(false);
        Boolean needAct = reActAgent.think();
        //调用工具之前判断状态
        if (isStopRequested()) {
            markCancelled();
            return STOPPED_ANSWER;
        }
        boolean isFinal = Boolean.FALSE.equals(needAct);
        reActAgent.setLastStepFinalAnswer(isFinal);

        // 推送思考区文案
        String thinkText = reActAgent.getLastThinkText();
        String display = StringUtils.defaultString(thinkText).trim();
        if (StringUtils.isBlank(display) && isFinal) {
            display = "已完成问题分析，正在组织最终回答…";
        }
        if (StringUtils.isNotBlank(display) || isFinal) {
            if (StringUtils.isNotBlank(display)) {
                sseEmitter.send(AgentStreamEvent.thinkingDelta(this.currentStep, display));
            }
        }

        if (isFinal) {
            this.status = AgentStatus.FINISHED;
            return AgentUserFacingFormatter.toAnswerDisplay(reActAgent.getFinalAnswer());
        }

        // 最后一步仍想调工具：跳过工具，直接综合，保证有结论
        if (lastStep) {
            sseEmitter.send(AgentStreamEvent.thinkingDelta(
                    this.currentStep, "已接近步骤上限，改为综合已有信息生成回答…"));
            this.status = AgentStatus.FINISHED;
            return synthesizeFinalAnswer(reActAgent);
        }

        //调用工具之后判断状态，是否需要推送后续信息
        if (isStopRequested()) {
            markCancelled();
            return STOPPED_ANSWER;
        }

        // 执行工具并推送用户可读摘要
        sseEmitter.send(AgentStreamEvent.thinkingProgress(
                this.currentStep, this.maxSteps, "正在调用工具…"));
        HitlContext.set(getChatId(), getUserId(), event -> {
            try {
                sseEmitter.send(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        HitlDecisionTracker.clear();
        String actSummary;
        try {
            actSummary = reActAgent.act();
        } finally {
            HitlContext.clear();
        }
        if (reActAgent instanceof ToolCallAgent toolCallAgent
                && (StringUtils.isNotBlank(toolCallAgent.getLastActDisplayText())
                || !toolCallAgent.getLastToolNames().isEmpty())) {
            if (StringUtils.isNotBlank(toolCallAgent.getLastActDisplayText())) {
                sseEmitter.send(AgentStreamEvent.thinkingDelta(
                        this.currentStep, toolCallAgent.getLastActDisplayText()));
            }
            List<String> names = toolCallAgent.getLastToolNames();
            List<String> raws = toolCallAgent.getLastToolRawResults();
            for (int i = 0; i < names.size(); i++) {
                String toolName = names.get(i);
                String raw = i < raws.size() ? raws.get(i) : null;
                if (!StringUtils.isNotBlank(raw)
                        && StringUtils.isNotBlank(toolCallAgent.getLastActDisplayText())) {
                    raw = toolCallAgent.getLastActDisplayText();
                }
                boolean rejected = HitlDecisionTracker.wasRejected(toolName)
                        || AgentUserFacingFormatter.isHitlRejectedResult(raw);
                String summary = rejected
                        ? AgentUserFacingFormatter.toolLabel(toolName) + " 已拒绝"
                        : AgentUserFacingFormatter.toolDoneSummary(toolName, raw);
                // 必须以摘要文案为准，避免 ThreadLocal 丢失时误标 success
                String outcome;
                if (rejected || summary.endsWith("已拒绝")) {
                    outcome = "rejected";
                } else if (summary.endsWith("已超时")) {
                    outcome = "timeout";
                } else if (summary.endsWith("失败") || summary.endsWith("已结束")) {
                    outcome = "failed";
                } else {
                    outcome = "success";
                }
                log.info("tool_done tool={}, outcome={}, summary={}, rawPrefix={}",
                        toolName, outcome, summary,
                        raw == null ? "null" : raw.substring(0, Math.min(80, raw.length())));
                sseEmitter.send(AgentStreamEvent.toolDone(
                        this.currentStep, toolName, summary, outcome));
            }
        } else if (StringUtils.isNotBlank(actSummary)
                && !AgentUserFacingFormatter.looksLikeRawToolDump(actSummary)) {
            sseEmitter.send(AgentStreamEvent.thinkingDelta(this.currentStep, actSummary));
        }
        HitlDecisionTracker.clear();

        // terminate：工具摘要不能当作最终答案，需再综合一轮
        if (this.status == AgentStatus.FINISHED) {
            return synthesizeFinalAnswer(reActAgent);
        }
        return null;
    }

    /**
     * 综合生成面向用户的最终回答（优先走 ToolCallAgent 无工具再生成）。
     *
     * @param reActAgent 当前智能体
     * @return 用户可读最终回答
     */
    private String synthesizeFinalAnswer(ReActAgent reActAgent) {
        if (reActAgent instanceof ToolCallAgent toolCallAgent) {
            return toolCallAgent.synthesizeUserFacingAnswer();
        }
        return AgentUserFacingFormatter.toAnswerDisplay(reActAgent.getFinalAnswer());
    }

    private boolean canContinueLoop() {
        return this.status != AgentStatus.FINISHED
                && this.status != AgentStatus.CANCELLED
                && this.status != AgentStatus.ERROR;
    }

    private boolean isStopRequested() {
        return stopSignalService != null
                && StringUtils.isNotBlank(chatId)
                && stopSignalService.shouldStop(chatId);
    }

    private void markCancelled() {
        this.status = AgentStatus.CANCELLED;
        log.info("Agent cancelled by stop signal, chatId={}, step={}/{}",
                chatId, currentStep, maxSteps);
    }

    /**
     * 同步模式下的单步执行，由子类实现具体逻辑。
     *
     * @return 本步执行结果文本
     */
    public abstract String step();

    /**
     * 清理本轮运行资源；清除停止信号，避免影响下一轮。
     */
    protected void cleanup() {
        if (stopSignalService != null && StringUtils.isNotBlank(chatId)) {
            stopSignalService.clear(chatId);
        }
    }
}
