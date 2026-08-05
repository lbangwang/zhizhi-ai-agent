package com.zhizhi.zhizhiaiagent.persistence.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeChunkItem;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeChunksResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeCitation;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeDocumentResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeRetrieveResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeSplitPreviewResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.KnowledgeDocumentEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.KnowledgeDocumentMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import com.zhizhi.zhizhiaiagent.rag.DocumentTextExtractor;
import com.zhizhi.zhizhiaiagent.rag.KnowledgeTextSplitter;
import com.zhizhi.zhizhiaiagent.rag.KnowledgeVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class KnowledgeDocumentService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".md", ".markdown", ".txt", ".docx", ".doc");

    /** 预览/查看切片时最多返回全文条数，避免超大响应 */
    public static final int MAX_CHUNK_RETURN = 200;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeTextSplitter knowledgeTextSplitter;
    private final KnowledgeVectorStoreService knowledgeVectorStoreService;
    private final DocumentTextExtractor documentTextExtractor;

    @Value("${app.knowledge.file-dir:data/knowledge-files}")
    private String fileDir;

    @Transactional
    public KnowledgeDocumentResponse upload(
            MultipartFile file,
            String userId,
            String title,
            KnowledgeTextSplitter.SplitParams splitParams) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "untitled.txt" : file.getOriginalFilename();
        validateExtension(originalName);

        String docId = IdGenerator.nextId();
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path target = Path.of(fileDir, userId, docId + "_" + safeName).toAbsolutePath().normalize();

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(docId);
        entity.setUserId(userId);
        entity.setTitle(StringUtils.hasText(title) ? title.trim() : stripExt(safeName));
        entity.setFilename(originalName);
        entity.setContentType(file.getContentType());
        entity.setFilePath(target.toString());
        entity.setChunkCount(0);
        entity.setStatus(1);
        AuditHelper.fillOnCreate(entity, userId, null);

        long uploadStart = System.currentTimeMillis();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);

            String text = documentTextExtractor.extract(target, originalName);
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("文件内容为空");
            }

            Map<String, Object> baseMeta = new HashMap<>();
            baseMeta.put("userId", userId);
            baseMeta.put("documentId", docId);
            baseMeta.put("filename", originalName);
            baseMeta.put("title", entity.getTitle());

            Document source = new Document(text, baseMeta);
            List<Document> chunks = knowledgeTextSplitter.split(List.of(source), splitParams);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("切片结果为空，请检查文件内容或调大切片参数");
            }

            List<String> chunkIds = new ArrayList<>();
            List<Document> toStore = new ArrayList<>();
            int index = 0;
            for (Document chunk : chunks) {
                String chunkId = IdGenerator.nextId();
                Map<String, Object> meta = new HashMap<>(baseMeta);
                meta.put("chunkIndex", index++);
                if (chunk.getMetadata() != null) {
                    meta.putAll(chunk.getMetadata());
                    meta.put("userId", userId);
                    meta.put("documentId", docId);
                    meta.put("filename", originalName);
                    meta.put("title", entity.getTitle());
                    meta.put("chunkIndex", index - 1);
                }
                toStore.add(new Document(chunkId, chunk.getText(), meta));
                chunkIds.add(chunkId);
            }

            knowledgeVectorStoreService.addAndPersist(toStore);

            entity.setChunkCount(chunkIds.size());
            entity.setChunkIds(JSONUtil.toJsonStr(chunkIds));
            knowledgeDocumentMapper.insert(entity);
            log.info("Knowledge uploaded docId={}, chunks={}, userId={}, costMs={}",
                    docId, chunkIds.size(), userId, System.currentTimeMillis() - uploadStart);
            return KnowledgeDocumentResponse.from(entity);
        } catch (IllegalArgumentException e) {
            cleanupFileQuietly(target);
            throw e;
        } catch (Exception e) {
            cleanupFileQuietly(target);
            log.error("Knowledge upload failed", e);
            throw new IllegalArgumentException("知识库入库失败: " + e.getMessage());
        }
    }

    /** 兼容旧调用：使用默认切分参数 */
    @Transactional
    public KnowledgeDocumentResponse upload(MultipartFile file, String userId, String title) {
        return upload(file, userId, title, null);
    }

    /**
     * 干跑切片：只提取文本并切分，不写库、不 embedding。
     */
    public KnowledgeSplitPreviewResponse previewSplit(
            MultipartFile file,
            KnowledgeTextSplitter.SplitParams splitParams) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "untitled.txt" : file.getOriginalFilename();
        validateExtension(originalName);

        Path temp = null;
        try {
            String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            temp = Files.createTempFile("kb-preview-", "-" + safeName);
            file.transferTo(temp);

            String text = documentTextExtractor.extract(temp, originalName);
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("文件内容为空");
            }

            KnowledgeTextSplitter.ResolvedSplitParams resolved = knowledgeTextSplitter.resolve(splitParams);
            Document source = new Document(text, Map.of("filename", originalName));
            List<Document> chunks = knowledgeTextSplitter.split(List.of(source), splitParams);

            List<KnowledgeChunkItem> items = new ArrayList<>();
            int limit = Math.min(chunks.size(), MAX_CHUNK_RETURN);
            for (int i = 0; i < limit; i++) {
                String chunkText = chunks.get(i).getText() == null ? "" : chunks.get(i).getText();
                items.add(KnowledgeChunkItem.builder()
                        .index(i)
                        .charCount(chunkText.length())
                        .text(chunkText)
                        .build());
            }

            return KnowledgeSplitPreviewResponse.builder()
                    .filename(originalName)
                    .extractedCharCount(text.length())
                    .strategy(resolved.getStrategy().wireName())
                    .params(toParamsMap(resolved))
                    .chunkCount(chunks.size())
                    .truncated(chunks.size() > MAX_CHUNK_RETURN)
                    .chunks(items)
                    .build();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Knowledge preview-split failed", e);
            throw new IllegalArgumentException("切片预览失败: " + e.getMessage());
        } finally {
            cleanupFileQuietly(temp);
        }
    }

    @Transactional(readOnly = true)
    public KnowledgeChunksResponse listChunks(String docId, String userId) {
        KnowledgeDocumentEntity entity = requireOwned(docId, userId);
        List<String> chunkIds = parseChunkIds(entity.getChunkIds());
        if (chunkIds.isEmpty()) {
            return KnowledgeChunksResponse.builder()
                    .documentId(docId)
                    .chunkCount(0)
                    .truncated(false)
                    .chunks(List.of())
                    .build();
        }

        List<Document> docs = knowledgeVectorStoreService.getByIds(chunkIds);
        docs.sort(Comparator.comparingInt(KnowledgeDocumentService::chunkIndexOf));

        List<KnowledgeChunkItem> items = new ArrayList<>();
        int limit = Math.min(docs.size(), MAX_CHUNK_RETURN);
        for (int i = 0; i < limit; i++) {
            Document doc = docs.get(i);
            String text = doc.getText() == null ? "" : doc.getText();
            int index = chunkIndexOf(doc);
            if (index < 0) {
                index = i;
            }
            items.add(KnowledgeChunkItem.builder()
                    .index(index)
                    .chunkId(doc.getId())
                    .charCount(text.length())
                    .text(text)
                    .build());
        }

        int total = entity.getChunkCount() != null ? entity.getChunkCount() : chunkIds.size();
        return KnowledgeChunksResponse.builder()
                .documentId(docId)
                .chunkCount(total)
                .truncated(docs.size() > MAX_CHUNK_RETURN || chunkIds.size() > MAX_CHUNK_RETURN)
                .chunks(items)
                .build();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> list(String userId) {
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getUserId, userId)
                        .orderByDesc(KnowledgeDocumentEntity::getUpdateDate))
                .stream()
                .map(KnowledgeDocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse get(String docId, String userId) {
        return KnowledgeDocumentResponse.from(requireOwned(docId, userId));
    }

    @Transactional
    public void delete(String docId, String userId) {
        KnowledgeDocumentEntity entity = requireOwned(docId, userId);
        List<String> chunkIds = parseChunkIds(entity.getChunkIds());
        if (!chunkIds.isEmpty()) {
            knowledgeVectorStoreService.deleteAndPersist(chunkIds);
        }
        cleanupFileQuietly(Path.of(entity.getFilePath()));
        knowledgeDocumentMapper.deleteById(entity.getId());
    }

    @Transactional(readOnly = true)
    public KnowledgeRetrieveResponse retrieve(String userId, String query, Integer topK) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("query 不能为空");
        }
        List<KnowledgeCitation> citations = knowledgeVectorStoreService.search(query, topK, userId);
        return KnowledgeRetrieveResponse.builder()
                .query(query.trim())
                .citations(citations)
                .build();
    }

    public static KnowledgeTextSplitter.SplitParams buildSplitParams(
            String splitStrategy,
            Integer chunkTokenSize,
            Integer paragraphMaxChars,
            Integer paragraphMinMergeChars,
            Integer minChunkLengthToEmbed,
            Integer maxNumChunks) {
        if (!StringUtils.hasText(splitStrategy)
                && chunkTokenSize == null
                && paragraphMaxChars == null
                && paragraphMinMergeChars == null
                && minChunkLengthToEmbed == null
                && maxNumChunks == null) {
            return null;
        }
        return KnowledgeTextSplitter.SplitParams.builder()
                .strategy(splitStrategy)
                .chunkTokenSize(chunkTokenSize)
                .paragraphMaxChars(paragraphMaxChars)
                .paragraphMinMergeChars(paragraphMinMergeChars)
                .minChunkLengthToEmbed(minChunkLengthToEmbed)
                .maxNumChunks(maxNumChunks)
                .build();
    }

    private static Map<String, Object> toParamsMap(KnowledgeTextSplitter.ResolvedSplitParams resolved) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("strategy", resolved.getStrategy().wireName());
        map.put("chunkTokenSize", resolved.getChunkTokenSize());
        map.put("minChunkSizeChars", resolved.getMinChunkSizeChars());
        map.put("minChunkLengthToEmbed", resolved.getMinChunkLengthToEmbed());
        map.put("maxNumChunks", resolved.getMaxNumChunks());
        map.put("paragraphMaxChars", resolved.getParagraphMaxChars());
        map.put("paragraphMinMergeChars", resolved.getParagraphMinMergeChars());
        return map;
    }

    private static int chunkIndexOf(Document doc) {
        if (doc == null || doc.getMetadata() == null) {
            return -1;
        }
        Object raw = doc.getMetadata().get("chunkIndex");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw != null) {
            try {
                return Integer.parseInt(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private KnowledgeDocumentEntity requireOwned(String docId, String userId) {
        KnowledgeDocumentEntity entity = knowledgeDocumentMapper.selectById(docId);
        if (entity == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("无权访问该文档");
        }
        return entity;
    }

    private static void validateExtension(String originalName) {
        String lower = originalName.toLowerCase(Locale.ROOT);
        String extension = extensionOf(lower);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("暂仅支持 .md / .txt / .docx / .doc 文件");
        }
    }

    private static List<String> parseChunkIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private static String extensionOf(String lowerFilename) {
        int i = lowerFilename.lastIndexOf('.');
        return i >= 0 ? lowerFilename.substring(i) : "";
    }

    private static void cleanupFileQuietly(Path path) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // ignore
        }
    }
}
