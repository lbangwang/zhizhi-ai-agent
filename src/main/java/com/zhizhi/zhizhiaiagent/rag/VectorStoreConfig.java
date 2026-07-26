package com.zhizhi.zhizhiaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 注册一个文档转化器
 * SimpleVectorStore 自带的
 */
@Configuration
public class VectorStoreConfig {

    @Resource
    private DocumentReaderConfig documentReaderConfig;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = documentReaderConfig.loadMarkdowns();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }

}
