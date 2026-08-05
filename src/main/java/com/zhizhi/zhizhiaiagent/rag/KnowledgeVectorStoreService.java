package com.zhizhi.zhizhiaiagent.rag;

import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeCitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * W2：基于 Spring AI {@link VectorStore}/{@link SimpleVectorStore} 的知识向量读写。
 * <p>
 * 入库时绕开 {@link SimpleVectorStore#doAdd} 的「逐条 embedding」实现，改为批量调用
 * {@link EmbeddingModel#embed(List)}，显著降低 DashScope 往返次数。
 */
@Slf4j
@Service
public class KnowledgeVectorStoreService {

    public static final String SYSTEM_USER_ID = "system";

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final Path storeFile;
    private final int defaultTopK;
    private final double similarityThreshold;
    /** 单次 embedding API 批量条数（通义一般建议 ≤25） */
    private final int embedBatchSize;

    public KnowledgeVectorStoreService(
            VectorStore loveAppVectorStore,
            EmbeddingModel dashscopeEmbeddingModel,
            @Value("${app.knowledge.vector-store-file:data/vector-store/knowledge-simple.json}") String storeFile,
            @Value("${app.knowledge.top-k:4}") int defaultTopK,
            @Value("${app.knowledge.similarity-threshold:0.45}") double similarityThreshold,
            @Value("${app.knowledge.embed-batch-size:16}") int embedBatchSize) {
        this.vectorStore = loveAppVectorStore;
        this.embeddingModel = dashscopeEmbeddingModel;
        this.storeFile = Path.of(storeFile).toAbsolutePath().normalize();
        this.defaultTopK = defaultTopK;
        this.similarityThreshold = similarityThreshold;
        this.embedBatchSize = Math.max(1, Math.min(embedBatchSize, 25));
    }

    public synchronized void addAndPersist(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
            addWithBatchEmbedding(simpleVectorStore, documents);
        } else {
            vectorStore.add(documents);
        }
        persist();
        log.info("VectorStore addAndPersist done, docs={}, batchSize={}, costMs={}",
                documents.size(), embedBatchSize, System.currentTimeMillis() - start);
    }

    /**
     * 批量 embedding 后写入 SimpleVectorStore 内存表，避免 N 次串行 HTTP。
     */
    @SuppressWarnings("unchecked")
    private void addWithBatchEmbedding(SimpleVectorStore store, List<Document> documents) {
        try {
            Field storeField = SimpleVectorStore.class.getDeclaredField("store");
            storeField.setAccessible(true);
            Map<String, Object> memory = (Map<String, Object>) storeField.get(store);
            Constructor<?> contentCtor = Class
                    .forName("org.springframework.ai.vectorstore.SimpleVectorStoreContent")
                    .getDeclaredConstructor(String.class, String.class, Map.class, float[].class);
            contentCtor.setAccessible(true);

            int total = documents.size();
            for (int from = 0; from < total; from += embedBatchSize) {
                int to = Math.min(from + embedBatchSize, total);
                List<Document> batch = documents.subList(from, to);
                List<String> texts = new ArrayList<>(batch.size());
                for (Document doc : batch) {
                    texts.add(doc.getText() == null ? "" : doc.getText());
                }

                long embedStart = System.currentTimeMillis();
                List<float[]> embeddings = embeddingModel.embed(texts);
                if (embeddings == null || embeddings.size() != batch.size()) {
                    throw new IllegalStateException("embedding 返回数量与切片不一致: expect="
                            + batch.size() + ", actual=" + (embeddings == null ? 0 : embeddings.size()));
                }
                log.info("Embedding batch {}-{} / {}, costMs={}",
                        from + 1, to, total, System.currentTimeMillis() - embedStart);

                for (int i = 0; i < batch.size(); i++) {
                    Document doc = batch.get(i);
                    String id = doc.getId();
                    if (!StringUtils.hasText(id)) {
                        throw new IllegalArgumentException("Document id 不能为空");
                    }
                    Map<String, Object> meta = doc.getMetadata() == null
                            ? Map.of()
                            : new LinkedHashMap<>(doc.getMetadata());
                    Object content = contentCtor.newInstance(
                            id,
                            doc.getText() == null ? "" : doc.getText(),
                            meta,
                            embeddings.get(i));
                    memory.put(id, content);
                }
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Batch embedding path failed, fallback to SimpleVectorStore.add: {}", e.getMessage());
            vectorStore.add(documents);
        }
    }

    public synchronized void deleteAndPersist(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        vectorStore.delete(chunkIds);
        persist();
    }

    /**
     * 按 ID 从 SimpleVectorStore 内存表读取文档（用于已入库切片预览）。
     * 返回顺序与传入 ids 一致；缺失的 ID 跳过。
     */
    public List<Document> getByIds(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }
        if (!(vectorStore instanceof SimpleVectorStore simpleVectorStore)) {
            log.warn("getByIds only supported for SimpleVectorStore, got {}", vectorStore.getClass().getName());
            return List.of();
        }
        Map<String, Document> byId = readStoreDocuments(simpleVectorStore);
        List<Document> result = new ArrayList<>();
        for (String id : chunkIds) {
            if (!StringUtils.hasText(id)) {
                continue;
            }
            Document doc = byId.get(id);
            if (doc != null) {
                result.add(doc);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Document> readStoreDocuments(SimpleVectorStore store) {
        try {
            Field field = SimpleVectorStore.class.getDeclaredField("store");
            field.setAccessible(true);
            Map<String, ?> raw = (Map<String, ?>) field.get(store);
            if (raw == null || raw.isEmpty()) {
                return Map.of();
            }
            Map<String, Document> docs = new LinkedHashMap<>();
            for (Map.Entry<String, ?> entry : raw.entrySet()) {
                Object content = entry.getValue();
                if (content == null) {
                    continue;
                }
                Method getId = content.getClass().getMethod("getId");
                Method getText = content.getClass().getMethod("getText");
                Method getMetadata = content.getClass().getMethod("getMetadata");
                String id = (String) getId.invoke(content);
                String text = (String) getText.invoke(content);
                Map<String, Object> metadata = (Map<String, Object>) getMetadata.invoke(content);
                if (!StringUtils.hasText(id)) {
                    continue;
                }
                docs.put(id, new Document(id, text == null ? "" : text,
                        metadata == null ? Map.of() : new LinkedHashMap<>(metadata)));
            }
            return docs;
        } catch (Exception e) {
            throw new IllegalStateException("读取 VectorStore 切片失败: " + e.getMessage(), e);
        }
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
