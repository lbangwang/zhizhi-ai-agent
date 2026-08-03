package com.zhizhi.zhizhiaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * W2：注册 Spring AI {@link VectorStore}（当前实现为 {@link SimpleVectorStore}）。
 * <p>
 * 优先加载本地持久化文件；不存在时可选灌入 classpath 种子 Markdown。
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Resource
    private DocumentReaderConfig documentReaderConfig;

    @Bean
    VectorStore loveAppVectorStore(
            EmbeddingModel dashscopeEmbeddingModel,
            @Value("${app.knowledge.vector-store-file:data/vector-store/knowledge-simple.json}") String storeFile,
            @Value("${app.knowledge.seed-classpath-docs:true}") boolean seedClasspathDocs) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        Path path = Path.of(storeFile).toAbsolutePath().normalize();
        File file = path.toFile();
        try {
            if (Files.isRegularFile(path) && Files.size(path) > 2) {
                simpleVectorStore.load(file);
                log.info("Loaded VectorStore from {}", path);
                return simpleVectorStore;
            }
        } catch (Exception e) {
            log.warn("Load VectorStore failed, will rebuild. path={}, err={}", path, e.getMessage());
        }

        if (seedClasspathDocs) {
            List<Document> seeded = enrichSeedDocuments(documentReaderConfig.loadMarkdowns());
            if (!seeded.isEmpty()) {
                simpleVectorStore.add(seeded);
                log.info("Seeded VectorStore with {} classpath documents", seeded.size());
            }
        }
        try {
            Files.createDirectories(path.getParent());
            simpleVectorStore.save(file);
            log.info("Initialized VectorStore file {}", path);
        } catch (Exception e) {
            log.warn("Save initial VectorStore failed: {}", e.getMessage());
        }
        return simpleVectorStore;
    }

    //读取本地文件，加载数据库
    private List<Document> enrichSeedDocuments(List<Document> source) {
        List<Document> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (Document doc : source) {
            Map<String, Object> meta = new HashMap<>(doc.getMetadata() == null ? Map.of() : doc.getMetadata());
            meta.putIfAbsent("userId", KnowledgeVectorStoreService.SYSTEM_USER_ID);
            meta.putIfAbsent("documentId", "seed");
            String filename = meta.get("filename") == null ? "classpath.md" : String.valueOf(meta.get("filename"));
            meta.putIfAbsent("filename", filename);
            meta.putIfAbsent("title", filename);
            String text = doc.getText();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            result.add(new Document(text, meta));
        }
        return result;
    }
}
