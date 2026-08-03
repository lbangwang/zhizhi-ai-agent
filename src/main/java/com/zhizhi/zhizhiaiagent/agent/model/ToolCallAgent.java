package com.zhizhi.zhizhiaiagent.agent.model;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zhizhi.zhizhiaiagent.agent.model.enums.AgentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 带工具调用能力的 ReAct Agent：负责思考选工具、执行工具、综合最终用户回答。
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent {

    private static final String TERMINATE_TOOL_NAME = "doTerminate";

    private static final String SYNTHESIZE_USER_PROMPT = """
            请基于以上信息，用中文、第一人称「我」自然地回复用户，给出最终答案。
            要求：
            1. 语气亲切流畅，不要生硬、不要像系统报告；结构用 Markdown 保持清晰；
            2. 可以适当润色，例如「我帮你整理了一下」「我经过梳理后，列举出了下面这些…」，然后进入正文；
            3. 不要输出 JSON；不要堆砌原始 URL；
            4. 不要写工具过程汇报，例如「已成功获取网页内容」「完全满足用户需求」
               「呈现给用户」「无需再调用其他工具」「现在即可输出最终回答」；
            5. 信息不足时用自然口吻说明，并给出建议。
            """;

    /** 可用工具集合 */
    private final ToolCallback[] availableTools;
    /** 工具执行管理器 */
    private final ToolCallingManager toolCallingManager;
    /** 模型调用选项（禁用框架内置自动工具执行） */
    private final ChatOptions chatOptions;
    /** 最近一次 think 的模型响应，供 act 读取 toolCalls */
    private ChatResponse toolCallChatResponse;
    /** 思考区展示文案 */
    private String lastThinkText = "";
    /** 行动区用户可读摘要 */
    private String lastActDisplayText = "";
    /** 最近计划/执行的工具名 */
    private List<String> lastToolNames = new ArrayList<>();

    /**
     * 使用默认 DashScope 工具代理选项构造 Agent。
     *
     * @param availableTools 可用工具
     */
    public ToolCallAgent(ToolCallback[] availableTools) {
        this(availableTools, DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build());
    }

    /**
     * 使用指定 ChatOptions 构造 Agent。
     *
     * @param availableTools 可用工具
     * @param chatOptions    模型选项
     */
    public ToolCallAgent(ToolCallback[] availableTools, ChatOptions chatOptions) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = chatOptions;
    }

    /**
     * 思考阶段：追加下一步提示、调用模型，解析是否需要工具调用，并生成思考区展示文案。
     *
     * @return true 表示需要执行 {@link #act()}；false 表示本轮已是最终回答
     */
    @Override
    public Boolean think() {
        if (StringUtils.isNotBlank(getNextStepPrompt())) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }

        try {
            Prompt prompt = new Prompt(getMessageList(), chatOptions);
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String modelText = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            log.info("{} 的思考: {}", getName(), modelText);
            log.info("{} 选择了 {} 个工具", getName(), toolCalls.size());

            // 组装用户可见思考文案
            StringBuilder display = new StringBuilder();
            String thinkPart = AgentUserFacingFormatter.toThinkingDisplay(modelText);
            if (StringUtils.isNotBlank(thinkPart)) {
                display.append(thinkPart);
            }
            List<String> toolNames = toolCalls.stream()
                    .map(AssistantMessage.ToolCall::name)
                    .collect(Collectors.toList());
            if (!toolCalls.isEmpty()) {
                String plan = AgentUserFacingFormatter.toToolPlanDisplay(toolNames);
                if (StringUtils.isNotBlank(plan)) {
                    if (!display.isEmpty()) {
                        display.append('\n');
                    }
                    display.append(plan);
                }
                String detail = toolCalls.stream()
                        .map(call -> call.name() + " args=" + call.arguments())
                        .collect(Collectors.joining("; "));
                log.info("tool plan detail: {}", detail);
            }
            this.lastThinkText = display.toString();
            this.lastToolNames = toolNames;

            if (toolCalls.isEmpty()) {
                getMessageList().add(assistantMessage);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("{} 的思考过程遇到了问题: {}", getName(), e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            this.lastThinkText = "思考过程出现异常，正在尝试恢复…";
            return false;
        }
    }

    /**
     * 行动阶段：执行工具调用，完整结果写入会话上下文，返回给前端的仅是可读摘要。
     *
     * @return 工具执行摘要文本；无工具调用时返回空串
     */
    @Override
    public String act() {
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            this.lastActDisplayText = "";
            return "";
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        long startedAt = System.currentTimeMillis();
        ToolExecutionResult executionResult =
                toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        long durationMs = System.currentTimeMillis() - startedAt;
        setMessageList(executionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage =
                (ToolResponseMessage) CollUtil.getLast(executionResult.conversationHistory());
        List<String> displayLines = new ArrayList<>();
        List<String> toolNames = new ArrayList<>();
        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            toolNames.add(response.name());
            displayLines.add(AgentUserFacingFormatter.toToolResultDisplay(
                    response.name(), response.responseData()));
            int rawLen = response.responseData() == null ? 0 : response.responseData().length();
            log.info("tool {} raw length={}", response.name(), rawLen);
        }
        this.lastToolNames = toolNames;
        this.lastActDisplayText = String.join("\n", displayLines);

        // 保存工具产物：工具审计 + 产物入库
        if (!Objects.isNull(getToolObservabilityService())) {
            try {
                List<AssistantMessage.ToolCall> toolCalls =
                        toolCallChatResponse.getResult().getOutput().getToolCalls();
                getToolObservabilityService().afterToolBatch(
                        getUserId(),
                        getChatId(),
                        toolCalls,
                        toolResponseMessage.getResponses(),
                        durationMs);
            } catch (Exception e) {
                log.warn("tool observability failed: {}", e.getMessage());
            }
        }

        boolean terminated = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> TERMINATE_TOOL_NAME.equals(response.name()));
        if (terminated) {
            setStatus(AgentStatus.FINISHED);
        }
        return this.lastActDisplayText;
    }

    /**
     * 综合最终回答：不调用工具，基于当前上下文生成面向用户的 Markdown 正文。
     * <p>
     * 用于步数上限、terminate 或兜底补齐结论。
     *
     * @return 清洗后的用户可读最终回答
     */
    public String synthesizeUserFacingAnswer() {
        getMessageList().add(new UserMessage(SYNTHESIZE_USER_PROMPT));
        try {
            ChatResponse response = getChatClient()
                    .prompt(new Prompt(getMessageList()))
                    .system(getSystemPrompt())
                    .call()
                    .chatResponse();
            AssistantMessage assistantMessage = response.getResult().getOutput();
            getMessageList().add(assistantMessage);
            this.lastThinkText = "正在整理最终回答…";
            return AgentUserFacingFormatter.toAnswerDisplay(assistantMessage.getText());
        } catch (Exception e) {
            log.error("{} 综合最终回答失败: {}", getName(), e.getMessage());
            return "抱歉，整理最终结论时出现异常，请稍后重试。";
        }
    }

    /**
     * 获取最近一次思考区展示文案。
     *
     * @return 思考文案，不会返回 null
     */
    @Override
    public String getLastThinkText() {
        return lastThinkText == null ? "" : lastThinkText;
    }

    /**
     * 获取最近一次行动摘要文案。
     *
     * @return 行动摘要，不会返回 null
     */
    public String getLastActDisplayText() {
        return lastActDisplayText == null ? "" : lastActDisplayText;
    }
}
