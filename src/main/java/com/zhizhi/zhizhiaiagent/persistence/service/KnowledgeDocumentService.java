package com.zhizhi.zhizhiaiagent.persistence.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeCitation;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeDocumentResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeRetrieveResponse;
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
import java.util.HashMap;
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

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeTextSplitter knowledgeTextSplitter;
    private final KnowledgeVectorStoreService knowledgeVectorStoreService;
    private final DocumentTextExtractor documentTextExtractor;

    @Value("${app.knowledge.file-dir:data/knowledge-files}")
    private String fileDir;

    @Transactional
    public KnowledgeDocumentResponse upload(MultipartFile file, String userId, String title) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "untitled.txt" : file.getOriginalFilename();
        String lower = originalName.toLowerCase(Locale.ROOT);
        String extension = extensionOf(lower);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("暂仅支持 .md / .txt / .docx / .doc 文件");
        }

        String docId = IdGenerator.nextId();
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path target = Path.of(fileDir, userId, docId + "_" + safeName).toAbsolutePath().normalize();

        //设置存储对象
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

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);

            //提取纯文本内容
            String text = documentTextExtractor.extract(target, originalName);
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("文件内容为空");
            }

            //转化为Document对象并切片
            Map<String, Object> baseMeta = new HashMap<>();
            baseMeta.put("userId", userId);
            baseMeta.put("documentId", docId);
            baseMeta.put("filename", originalName);
            baseMeta.put("title", entity.getTitle());

            Document source = new Document(text, baseMeta);

            //切片
            List<Document> chunks = knowledgeTextSplitter.split(List.of(source));
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("切片结果为空，请检查文件内容");
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
                }
                toStore.add(new Document(chunkId, chunk.getText(), meta));
                chunkIds.add(chunkId);
            }

            knowledgeVectorStoreService.addAndPersist(toStore);

            //设置文档对象中切片大小和切片ids
            entity.setChunkCount(chunkIds.size());
            entity.setChunkIds(JSONUtil.toJsonStr(chunkIds));
            knowledgeDocumentMapper.insert(entity);
            log.info("Knowledge uploaded docId={}, chunks={}, userId={}", docId, chunkIds.size(), userId);
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
