package com.zhizhi.zhizhiaiagent.agent.model;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zhizhi.zhizhiaiagent.advisor.MyLogAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 枝枝超级智能体：装配提示词、工具与 ChatClient，具体推理循环由父类完成。
 */
@Component
public class ZhizhiManus extends ToolCallAgent {

    private static final int DEFAULT_MAX_STEPS = 8;

    private static final String SYSTEM_PROMPT = """
            你是 ZhizhiManus（枝枝超级智能体），和用户一对一聊天的中文助手。请用第一人称「我」自然地和对方说话（可用「你」）。
            
            工作方式：
            1. 先弄清用户想要什么，再决定要不要用工具；工具只是你私下查资料的手段；
            2. 复杂问题可以分步做，但每步只做有必要的事；
            3. 最终回复要像真人助手：语气自然、流畅，结构清晰（Markdown：标题、列表、加粗均可）；
            4. 可以适当润色引导，例如「我帮你整理了一下」「我经过梳理后，列举出了下面这些…」；
            5. 信息不够就坦诚说，并给出可继续问的方向；不要编造。
            
            【产物类硬性要求】
            - 用户要求生成 PDF / 导出文件 / 下载资源时：必须调用对应工具（generatePDF / writeFile / downloadResource），
              不能只在对话里用文字假装“已生成”；
            - 可先 searchWeb 收集内容，但信息够后务必再调用 generatePDF（或相应文件工具）写出真实文件；
            - 工具返回成功路径后，再给用户最终回复，并说明已生成文件（文件名即可）。
            
            【最终回答怎么写】
            - 面向用户写「答案本身」，语言轻松一点，别太公文、别太生硬；
            - 开头可用一两句自然过渡，然后进入条理清楚的正文；
            - 不要写系统旁白或工具汇报，例如：
              「已成功获取网页内容」「完全满足用户需求」「呈现给用户」
              「无需再调用其他工具」「现在即可输出最终回答」「根据工具返回结果」；
            - 不要站在第三方评价「是否满足用户需求」，你就是在跟用户聊天。
            
            信息够了、且用户未要求额外文件产物时，直接给出最终 Markdown 回答，不必再调工具。
            只有需要明确结束任务时，才调用 terminate。
            """;

    private static final String NEXT_STEP_PROMPT = """
            按用户需求选择合适工具；复杂任务可拆步。
            工具执行后：
            - 若用户要 PDF/文件：在尚未成功调用 generatePDF/writeFile/downloadResource 前，不要结束；
              搜完资料后请继续调用 generatePDF（或对应工具）生成真实文件；
            - 若已能回答且产物已生成（或不需要产物）：不要再调工具；用第一人称写自然、流畅的 Markdown 最终答案。
            - 若还不够：再选下一步工具。
            不要写「已获取网页 / 满足用户需求 / 无需再调用工具」这类过程汇报。
            结束任务时使用 terminate。
            """;

    /**
     * Spring 注入构造：默认使用千问模型与工具代理选项。
     *
     * @param allTools             全部可用工具
     * @param dashscopeChatModel   默认 ChatModel
     */
    @Autowired
    public ZhizhiManus(ToolCallback[] allTools,
                       @Qualifier("dashscopeChatModel") ChatModel dashscopeChatModel) {
        this(allTools, dashscopeChatModel, DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build());
    }

    /**
     * 完整构造：绑定工具、模型与选项，并初始化 Agent 配置。
     *
     * @param allTools    可用工具
     * @param chatModel   对话模型
     * @param chatOptions 模型选项
     */
    public ZhizhiManus(ToolCallback[] allTools, ChatModel chatModel, ChatOptions chatOptions) {
        super(allTools, chatOptions);
        this.setName("ZhizhiManus");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(DEFAULT_MAX_STEPS);
        this.setChatClient(ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLogAdvisor())
                .build());
    }


    /**
     * 增加上下文记忆
     *
     * @param allTools    可用工具
     * @param chatModel   对话模型
     * @param chatOptions 模型选项
     * @param chatId chatId
     */
    public ZhizhiManus(ToolCallback[] allTools, ChatModel chatModel, ChatOptions chatOptions, String chatId) {
        super(allTools, chatOptions);
        this.setName("ZhizhiManus");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(DEFAULT_MAX_STEPS);
        this.setChatClient(ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLogAdvisor())
                .defaultAdvisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .build());
    }
}
