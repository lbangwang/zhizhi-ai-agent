package com.zhizhi.zhizhiaiagent.app;


import com.zhizhi.zhizhiaiagent.advisor.MyLogAdvisor;
import com.zhizhi.zhizhiaiagent.advisor.ReReadingAdvisor;
import com.zhizhi.zhizhiaiagent.chatMemory.FileBasedChatMemory;
import com.zhizhi.zhizhiaiagent.config.ChatModelRouter;
import com.zhizhi.zhizhiaiagent.model.ChatModelType;
import com.zhizhi.zhizhiaiagent.rag.LoveAppContextualQueryAugmenterFactory;
import com.zhizhi.zhizhiaiagent.rag.LoveAppRagCustomAdvisorFactory;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.FormatProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Converter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * AI面试官小助手CC：AI 应用开发求职与面试辅导智能体。
 */
@Component
@Slf4j
public class LoveApp {
    private final ChatClient chatClient;
    private final ChatModelRouter chatModelRouter;
    private final ChatMemory chatMemory;
    private final Map<ChatModelType, ChatClient> chatClientCache = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = "您好，我是专注于AI应用开发领域的AI面试官小助手CC！" +
            "深耕AI技术核心板块，对技术MCP、RAG、Prompt优化、Function Calling，以及AI框架LangChain等均有深厚积累与实践经验。\n" +
            "无论您在求职AI应用开发工程师岗位时，面临技术方案设计、项目经验梳理、面试难题拆解，或是想优化技术简历、打磨实战项目，都能向我倾诉。我会结合求职场景，精准聚焦痛点，引导您详述求职需求、技能短板与目标岗位细节，为您量身定制专属求职策略，助力高效斩获心仪offer！";

    public LoveApp(ChatModelRouter chatModelRouter) {
        this.chatModelRouter = chatModelRouter;
        this.chatMemory = new InMemoryChatMemory();
        // 默认使用千问，兼容旧调用
        this.chatClient = buildChatClient(chatModelRouter.resolve(ChatModelType.QWEN.getCode()));
        this.chatClientCache.put(ChatModelType.QWEN, this.chatClient);
    }

    private ChatClient buildChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultAdvisors(new MyLogAdvisor())
                .build();
    }

    private ChatClient getChatClient(String model) {
        ChatModelType type = ChatModelType.from(model);
        return chatClientCache.computeIfAbsent(type,
                key -> buildChatClient(chatModelRouter.resolve(key.getCode())));
    }



    public String doChat(String userMessage, String conversationId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(userMessage)
                //第一个参数设置会话ID，第二个参数即保存用户记忆会话长度
                .advisors(advisor -> advisor.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("AI xl content:{}", content);

        Usage usage = chatResponse.getMetadata().getUsage();
        //提示词消耗token
        Integer promptTokens = usage.getPromptTokens();
        //响应消耗token数量
        Integer completionTokens = usage.getCompletionTokens();
        log.info("promptTokens:{},completionTokens:{}", promptTokens, completionTokens);

        return content;
    }


    record LoveReport(String title, List<String> suggestions) {
    }

    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成面试辅导结果，标题为{用户名}的面试辅导报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }


    @Autowired
    private VectorStore vectorStore;

    public ChatResponse doChatRag(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(new MyLogAdvisor())
                //使用rag本地知识库进行增强
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .chatResponse();
    }

    @Autowired
    private Advisor loveAppRagCloudAdvisor;

    @Autowired
    private ToolCallback[] toolCallbacks;

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLogAdvisor())
                // 应用增强检索服务（云知识库服务）
                .advisors(loveAppRagCloudAdvisor)
                //筛选条件
//                .advisors(LoveAppRagCustomAdvisorFactory.
//                        createLoveAppRagCustomAdvisor(vectorStore, "单身"))

                //添加工具类
                .tools(toolCallbacks)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLogAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    // 同步客户端
//    @Autowired
//    private List<McpSyncClient> mcpSyncClients;
//
//    // 异步客户端
//    @Autowired
//    private List<McpAsyncClient> mcpAsyncClients;

    // 和 Spring AI 的工具进行整合
    @Autowired
    private SyncMcpToolCallbackProvider syncMcpToolCallbackProvider;


    public String doChatWithMcpTest(String message, String chatId) {
        ToolCallback[] toolCallbackProviderToolCallbacks = syncMcpToolCallbackProvider.getToolCallbacks();
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLogAdvisor())
                .tools(toolCallbackProviderToolCallbacks)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    // 使用AtomicBoolean支持同一会话多次请求
    private final Map<String, AtomicBoolean> sessionStates = new ConcurrentHashMap<>();

    /**
     * 流式输出（默认千问）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return doChatByStream(message, chatId, ChatModelType.QWEN.getCode());
    }

    /**
     * 流式输出，按 model 路由到不同大模型。
     *
     * @param model deepseek / qwen / doubao
     */
    public Flux<String> doChatByStream(String message, String chatId, String model) {
        // 参数校验
        if (message == null || message.trim().isEmpty()) {
            log.warn("Session {} received empty message", chatId);
            return Flux.just("消息不能为空");
        }

        if (chatId == null || chatId.trim().isEmpty()) {
            log.warn("Received request with empty chatId");
            return Flux.just("会话ID不能为空");
        }

        final ChatClient selectedClient;
        try {
            selectedClient = getChatClient(model);
        } catch (Exception e) {
            log.error("Resolve chat model failed, model={}", model, e);
            return Flux.just(e.getMessage());
        }

        log.info("Session {} using model={}", chatId, model);

        //添加对于的会话状态，如果不存在则创建一个新的AtomicBoolean对象
        AtomicBoolean state = sessionStates.computeIfAbsent(chatId,
                k -> new AtomicBoolean(true));
        // 重置状态为true（允许新的流）
        state.set(true);

        ChatClient.ChatClientRequestSpec promptSpec = selectedClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLogAdvisor());
        // 云知识库顾问仅适配千问 / DashScope
        if (chatModelRouter.isQwen(model)) {
            promptSpec = promptSpec.advisors(loveAppRagCloudAdvisor);
        }
        Flux<String> contentFlux = promptSpec
                .tools(toolCallbackProvider)
                .stream()
                .content();
        // 应用控制逻辑
        return contentFlux
                // 关键：检查是否允许继续输出
                .takeWhile(content -> {
                    // 检查状态
                    boolean canContinue = state.get();
                    if (!canContinue) {
                        log.debug("Session {} stopped by user", chatId);
                    }
                    return canContinue;
                })
                // 在流结束时清理或重置状态
                .doFinally(signalType -> {
                    // 重置状态为true供下次使用（而不是删除）
                    state.set(true);
                    log.debug("Session {} finished with signal: {}, state reset for next use",
                            chatId, signalType);
                })
                // 错误处理
                .onErrorResume(throwable -> {
                    log.error("Stream error for session {}", chatId, throwable);
                    // 重置状态，允许重试
                    state.set(true);
                    // 返回友好的错误信息
                    return Flux.just("系统处理异常，请稍后重试。错误信息：" + throwable.getMessage());
                })
                // 添加超时保护（5分钟）
                .timeout(java.time.Duration.ofMinutes(5))
                .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
                    log.warn("Session {} timeout", chatId);
                    state.set(true);
                    return Flux.just("请求超时，请重试");
                });
    }


    /**
     * 停止会话
     *
     * @param chatId 会话ID
     */
    public void stopChat(String chatId) {
        // 1. 参数校验
        if (chatId == null || chatId.trim().isEmpty()) {
            log.warn("Attempt to stop chat with empty chatId");
            return;
        }

        log.info("Stopping session: {}", chatId);

        // 2. 检查会话是否存在
        AtomicBoolean state = sessionStates.get(chatId);
        if (state == null) {
            log.warn("No active session found for {}", chatId);
            return;
        }

        // 3. 设置状态为false，停止输出
        state.set(false);
    }

}
