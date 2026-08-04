package com.zhizhi.zhizhiaiagent.agent.multi;

import com.zhizhi.zhizhiaiagent.advisor.MyLogAdvisor;
import com.zhizhi.zhizhiaiagent.agent.model.ToolCallAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;

/**
 * Worker：短步数 ToolCallAgent，只完成 Planner 下发的当前一步。
 */
public class WorkerAgent extends ToolCallAgent {

    private static final int DEFAULT_MAX_STEPS = 4;

    private static final String SYSTEM_PROMPT = """
            你是执行 Agent（Worker）。用户消息是「当前这一步」的任务说明。
            规则：
            1. 只完成当前这一步，不要擅自做后续步骤或额外产物；
            2. 需要工具时再调用；写文件/终端会触发人机确认，请按工具结果如实回复；
            3. 完成后用中文简要说明本步结果（Markdown 可）；不要汇报「已满足全部需求」之类全局结论；
            4. 本步结束后调用 terminate。
            """;

    private static final String NEXT_STEP_PROMPT = """
            专注当前步骤：需要工具就调用；已完成则 terminate，并用第一人称简要汇报本步结果。
            """;

    public WorkerAgent(ToolCallback[] tools, ChatModel chatModel, ChatOptions chatOptions) {
        super(tools, chatOptions);
        this.setName("WorkerAgent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(DEFAULT_MAX_STEPS);
        this.setChatClient(ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLogAdvisor())
                .build());
    }

    /**
     * 多 Agent 共用同一 chatId 停止信号，不能在单步 Worker 结束时清掉。
     */
    @Override
    protected void cleanup() {
        // no-op：由 MultiAgentOrchestrator 在整轮结束时 clear
    }
}
