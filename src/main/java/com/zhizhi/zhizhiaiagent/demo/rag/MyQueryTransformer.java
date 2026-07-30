package com.zhizhi.zhizhiaiagent.demo.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 预检索阶段对用户提示词做增强（重写 / 翻译 / 压缩）。
 * <p>
 * 注意：Spring AI 默认 Rewrite 模板是英文，容易把中文问题改写成英文；
 * 本类使用中文改写模板，并强制与原问题保持同一语言。
 */
@Slf4j
@Component
public class MyQueryTransformer {

    /**
     * 中文友好的查询改写模板。占位符 {target}、{query} 为 RewriteQueryTransformer 必需。
     */
    private static final PromptTemplate CHINESE_REWRITE_PROMPT = new PromptTemplate("""
            你是查询改写助手。请把用户的原始问题改写成更清晰、具体、便于回答的提示词。

            硬性要求：
            1. 必须与原问题使用相同语言：中文问题必须输出中文，禁止翻译成英文或其他语言；
            2. 去掉无意义语气词、重复字符（如「哈哈哈哈」「啊啊啊」），保留真实意图；
            3. 纠正明显错别字或术语被空格拆开的问题（如「L ang Chain」→「LangChain」）；
            4. 不改变用户核心诉求，不要扩写成教程正文或直接作答；
            5. 只输出改写后的问题本身，不要解释、不要加引号、不要前后缀。

            面向场景：{target}

            原始问题：
            {query}

            改写后的问题：
            """);

    private static final String REWRITE_TARGET = "中文 AI 助手对话与知识检索";

    /**
     * 对用户原始输入做提示词重写增强；失败或语言被改写时回退为原文。
     *
     * @param message   用户原始问题
     * @param chatModel 用于重写的大模型
     * @return 增强后的提示词；失败则返回原文
     */
    public String rewriteUserMessage(String message, ChatModel chatModel) {
        if (StringUtils.isBlank(message) || chatModel == null) {
            return message;
        }
        try {
            ChatClient.Builder builder = ChatClient.builder(chatModel);
            Query rewritten = rewriteQueryTransformer(
                    Query.builder().text(message.trim()).build(),
                    builder);
            if (rewritten == null || StringUtils.isBlank(rewritten.text())) {
                return message;
            }
            String enhanced = rewritten.text().trim();
            // 防护：原文含中文，但改写结果变成纯英文时，回退原文，避免整段对话变成英文
            if (containsChinese(message) && !containsChinese(enhanced)) {
                log.warn("Query rewrite changed language to English, fallback to original. original={}, enhanced={}",
                        message, enhanced);
                return message;
            }
            log.info("Query rewrite success. original={}, enhanced={}", message, enhanced);
            return enhanced;
        } catch (Exception e) {
            log.warn("Query rewrite failed, fallback to original message. original={}", message, e);
            return message;
        }
    }

    /**
     * 预处理：重写提示词，使其更清晰、适合后续检索与回答（保持原语言，默认中文场景）。
     *
     * @param query             原始查询
     * @param chatClientBuilder 用于重写的 ChatClient 构建器
     * @return 重写后的查询
     */
    public Query rewriteQueryTransformer(Query query, ChatClient.Builder chatClientBuilder) {
        RewriteQueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(CHINESE_REWRITE_PROMPT)
                .targetSearchSystem(REWRITE_TARGET)
                .build();
        Query apply = rewriteQueryTransformer.apply(query);
        log.info("RewriteQueryTransformer: {}", apply.text());
        return apply;
    }

    /**
     * 预处理：查询翻译为目标语言。
     *
     * @param query             原始查询
     * @param chatClientBuilder 用于翻译的 ChatClient 构建器
     * @return 翻译后的查询
     */
    public Query translationQueryTransformer(Query query, ChatClient.Builder chatClientBuilder) {
        TranslationQueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage("English")
                .build();
        Query apply = translationQueryTransformer.apply(query);
        log.info("TranslationQueryTransformer: {}", apply.text());
        return apply;
    }

    /**
     * 预处理：压缩历史上下文，生成简洁查询。
     *
     * @param query             原始查询
     * @param chatClientBuilder 用于压缩的 ChatClient 构建器
     * @return 压缩后的查询
     */
    public Query compressionQueryTransformer(Query query, ChatClient.Builder chatClientBuilder) {
        CompressionQueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        Query apply = compressionQueryTransformer.apply(query);
        log.info("CompressionQueryTransformer: {}", apply.text());
        return apply;
    }

    private static boolean containsChinese(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return text.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }
}
