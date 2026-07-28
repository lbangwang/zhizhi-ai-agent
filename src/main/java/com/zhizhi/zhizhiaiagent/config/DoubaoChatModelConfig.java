package com.zhizhi.zhizhiaiagent.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 豆包（火山方舟）OpenAI 兼容接口。
 * <p>
 * 注意：base-url 应为 https://ark.cn-beijing.volces.com/api/v3，
 * completions-path 应为 /chat/completions（不能用 OpenAI 默认的 /v1/chat/completions）。
 */
@Configuration
public class DoubaoChatModelConfig {

    @Bean(name = "doubaoChatModel")
    @ConditionalOnProperty(prefix = "spring.ai.doubao", name = "api-key")
    public OpenAiChatModel doubaoChatModel(
            @Value("${spring.ai.doubao.api-key}") String apiKey,
            @Value("${spring.ai.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}") String baseUrl,
            @Value("${spring.ai.doubao.chat.options.model}") String model,
            @Value("${spring.ai.doubao.chat.completions-path:/chat/completions}") String completionsPath) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
