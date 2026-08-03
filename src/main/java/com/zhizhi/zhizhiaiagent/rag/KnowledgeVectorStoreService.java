package com.zhizhi.zhizhiaiagent.rag;

import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeCitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * W2：基于 Spring AI {@link VectorStore}/{@link SimpleVectorStore} 的知识向量读写。
 * <p>
 * 负责切片的增删、落盘，以及按用户过滤的相似度检索。
 */
@Slf4j
@Service
public class KnowledgeVectorStoreService {


    public static final String SYSTEM_USER_ID = "system";

    /** 内存中的向量库实例（当前为 SimpleVectorStore Bean：loveAppVectorStore） */
    private final VectorStore vectorStore;

    /** SimpleVectorStore 持久化 JSON 文件的绝对路径 */
    private final Path storeFile;

    /** 检索默认返回条数 */
    private final int defaultTopK;

    /** 相似度阈值：低于该分的片段不返回（0~1，越大越严格） */
    private final double similarityThreshold;

    /**
     * @param loveAppVectorStore   Spring 注入 Bean（名称 loveAppVectorStore）
     * @param storeFile            向量库落盘路径，
     * @param defaultTopK          默认召回条数，
     * @param similarityThreshold  相似度下限，默认 0.45；
     *                             过滤掉相关度太低的噪声片段
     */
    public KnowledgeVectorStoreService(
            VectorStore loveAppVectorStore,
            @Value("${app.knowledge.vector-store-file:data/vector-store/knowledge-simple.json}") String storeFile,
            @Value("${app.knowledge.top-k:4}") int defaultTopK,
            @Value("${app.knowledge.similarity-threshold:0.45}") double similarityThreshold) {
        this.vectorStore = loveAppVectorStore;
        this.storeFile = Path.of(storeFile).toAbsolutePath().normalize();
        this.defaultTopK = defaultTopK;
        this.similarityThreshold = similarityThreshold;
    }

    public synchronized void addAndPersist(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        vectorStore.add(documents);
        persist();
    }

    public synchronized void deleteAndPersist(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        vectorStore.delete(chunkIds);
        persist();
    }

    public List<KnowledgeCitation> search(String query, Integer topK, String userId) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        int k = topK != null && topK > 0 ? topK : defaultTopK;
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query.trim())
                .topK(k)
                .similarityThreshold(similarityThreshold);

        if (StringUtils.hasText(userId)) {
            FilterExpressionBuilder fb = new FilterExpressionBuilder();
            builder.filterExpression(fb.or(
                    fb.eq("userId", userId.trim()),
                    fb.eq("userId", SYSTEM_USER_ID)
            ).build());
        }

        List<Document> docs = vectorStore.similaritySearch(builder.build());
        List<KnowledgeCitation> citations = new ArrayList<>();
        for (Document doc : docs) {
            Map<String, Object> meta = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
            citations.add(KnowledgeCitation.builder()
                    .documentId(asString(meta.get("documentId")))
                    .chunkId(doc.getId())
                    .filename(asString(meta.get("filename")))
                    .title(asString(meta.get("title")))
                    .snippet(trimSnippet(doc.getText()))
                    .score(doc.getScore())
                    .build());
        }
        return citations;
    }

    public synchronized void persist() {
        try {
            Files.createDirectories(storeFile.getParent());
            File file = storeFile.toFile();
            if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
                simpleVectorStore.save(file);
                log.info("VectorStore persisted to {}", storeFile);
            }
        } catch (Exception e) {
            log.warn("Persist VectorStore failed: {}", e.getMessage());
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String trimSnippet(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() > 400 ? normalized.substring(0, 400) + "…" : normalized;
    }
}
