package com.zhizhi.zhizhiaiagent.demo.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 预检索阶段，对用户prompt提示词进行增强
 */
@Slf4j
@Component
public class MyQueryTransformer {

    /**
     * 预处理，重写提示词
     * @return
     */
    public Query rewriteQueryTransformer(Query query,ChatClient.Builder chatClientBuilder){
        RewriteQueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        Query apply = rewriteQueryTransformer.apply(query);
        log.info("RewriteQueryTransformer: {}",apply.text());
        return apply;
    }

    /**
     * 预处理，查询翻译
     * @return
     */
    public Query translationQueryTransformer(Query query,ChatClient.Builder chatClientBuilder){
        TranslationQueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage("English") //目标语言
                .build();
        Query apply = translationQueryTransformer.apply(query);
        log.info("TranslationQueryTransformer: {}",apply.text());
        return apply;
    }

    /**
     * 预处理，查询压缩，提取历史对话的关键信息，生成简洁的查询
     * @return
     */
    public Query compressionQueryTransformer(Query query,ChatClient.Builder chatClientBuilder){
        CompressionQueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        Query apply = compressionQueryTransformer.apply(query);
        log.info("CompressionQueryTransformer: {}",apply.text());
        return apply;
    }
}
