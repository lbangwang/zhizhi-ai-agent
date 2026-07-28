package com.zhizhi.zhizhiaiagent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zhizhi.zhizhiaiagent.model.ChatModelType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 按前端 model 参数路由到对应 ChatModel / ChatOptions。
 */
@Service
public class ChatModelRouter {

    private final ChatModel dashscopeChatModel;
    private final ChatModel deepSeekChatModel;
    private final ChatModel doubaoChatModel;

    public ChatModelRouter(
            @Qualifier("dashscopeChatModel") ChatModel dashscopeChatModel,
            @Autowired(required = false) @Qualifier("deepSeekChatModel") ChatModel deepSeekChatModel,
            @Autowired(required = false) @Qualifier("doubaoChatModel") ChatModel doubaoChatModel) {
        this.dashscopeChatModel = dashscopeChatModel;
        this.deepSeekChatModel = deepSeekChatModel;
        this.doubaoChatModel = doubaoChatModel;
    }

    public ChatModel resolve(String modelParam) {
        ChatModelType type = ChatModelType.from(modelParam);
        return switch (type) {
            case QWEN -> dashscopeChatModel;
            case DEEPSEEK -> {
                if (deepSeekChatModel == null) {
                    throw new IllegalStateException("DeepSeek 模型未配置，请在 application.yml 设置 spring.ai.openai.api-key");
                }
                yield deepSeekChatModel;
            }
            case DOUBAO -> {
                if (doubaoChatModel == null) {
                    throw new IllegalStateException("豆包模型未配置，请在 application.yml 设置 spring.ai.doubao.api-key");
                }
                yield doubaoChatModel;
            }
        };
    }

    public ChatOptions resolveChatOptions(String modelParam) {
        ChatModelType type = ChatModelType.from(modelParam);
        return switch (type) {
            case QWEN -> DashScopeChatOptions.builder()
                    .withProxyToolCalls(true)
                    .build();
            case DEEPSEEK, DOUBAO -> OpenAiChatOptions.builder()
                    .proxyToolCalls(true)
                    .internalToolExecutionEnabled(false)
                    .build();
        };
    }

    public boolean isQwen(String modelParam) {
        return ChatModelType.from(modelParam) == ChatModelType.QWEN;
    }
}
