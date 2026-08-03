package com.zhizhi.zhizhiaiagent.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.ArtifactResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.ArtifactEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.ArtifactMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class ArtifactService {

    private static final Pattern PATH_IN_RESULT = Pattern.compile(
            "(?i)successfully to:\\s*(.+?)\\s*$");

    private final ArtifactMapper artifactMapper;

    @Value("${app.artifact.file-dir:data/artifacts}")
    private String fileDir;

    /** Spring AI 工具结果常带 JSON 字符串引号，例如 {@code "PDF generated successfully to: /tmp/a.pdf"} */
    static String unwrapToolResultText(String resultText) {
        if (!StringUtils.hasText(resultText)) {
            return resultText;
        }
        String value = resultText.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            // 常见转义
            value = value.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value.trim();
    }

    public List<ArtifactResponse> listByChatId(String userId, String chatId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LambdaQueryWrapper<ArtifactEntity> q = new LambdaQueryWrapper<ArtifactEntity>()
                .eq(ArtifactEntity::getUserId, userId)
                .orderByDesc(ArtifactEntity::getCreateDate);
        if (StringUtils.hasText(chatId)) {
            q.eq(ArtifactEntity::getChatId, chatId.trim());
        }
        return artifactMapper.selectList(q).stream()
                .map(ArtifactResponse::from)
                .collect(Collectors.toList());
    }

    public ArtifactEntity requireOwned(String id, String userId) {
        ArtifactEntity entity = artifactMapper.selectById(IdGenerator.requireId(id, "id"));
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("产物不存在或无权访问");
        }
        return entity;
    }

    public Resource loadAsResource(String id, String userId) {
        ArtifactEntity entity = requireOwned(id, userId);
        Path path = Path.of(entity.getFilePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("产物文件已丢失");
        }
        return new FileSystemResource(path);
    }

    public String resolveContentType(ArtifactEntity entity) {
        if (StringUtils.hasText(entity.getContentType())) {
            return entity.getContentType();
        }
        return guessContentType(entity.getFileName());
    }

    /**
     * 从工具结果中解析本地路径并入库；非产物工具或解析失败时返回 empty。
     */
    @Transactional
    public Optional<ArtifactResponse> tryRegisterFromToolResult(
            String userId,
            String chatId,
            String toolName,
            String resultText) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(resultText)) {
            return Optional.empty();
        }
        if (!isArtifactProducingTool(toolName)) {
            return Optional.empty();
        }
        String normalized = unwrapToolResultText(resultText);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("error")
                || lower.contains("not recognized")
                || !lower.contains("successfully to:")) {
            return Optional.empty();
        }

        String sourcePath = extractPath(normalized);
        if (!StringUtils.hasText(sourcePath)) {
            log.warn("artifact path not found in tool result, tool={}, result={}", toolName, normalized);
            return Optional.empty();
        }

        Path source = Path.of(sourcePath.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            log.warn("artifact source missing: tool={}, path={}", toolName, source);
            return Optional.empty();
        }

        try {
            String artifactId = IdGenerator.nextId();
            String fileName = source.getFileName().toString();
            String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path target = Path.of(fileDir, userId, artifactId + "_" + safeName)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            long size = Files.size(target);
            ArtifactEntity entity = new ArtifactEntity();
            entity.setId(artifactId);
            entity.setUserId(userId);
            entity.setChatId(StringUtils.hasText(chatId) ? chatId.trim() : null);
            entity.setToolName(toolName);
            entity.setFileName(fileName);
            entity.setContentType(guessContentType(fileName));
            entity.setFilePath(target.toString());
            entity.setFileSize(size);
            entity.setSourcePath(source.toString());
            AuditHelper.fillOnCreate(entity, userId, null);
            artifactMapper.insert(entity);
            log.info("artifact registered id={}, tool={}, file={}", artifactId, toolName, fileName);
            return Optional.of(ArtifactResponse.from(entity));
        } catch (IOException e) {
            log.error("register artifact failed tool={}: {}", toolName, e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean isArtifactProducingTool(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return false;
        }
        return switch (toolName) {
            case "generatePDF", "writeFile", "downloadResource" -> true;
            default -> false;
        };
    }

    private static String extractPath(String resultText) {
        String text = unwrapToolResultText(resultText);
        Matcher matcher = PATH_IN_RESULT.matcher(text);
        if (matcher.find()) {
            return stripWrappingQuotes(matcher.group(1).trim());
        }
        // 兜底：结果末尾像绝对路径
        int idx = Math.max(text.lastIndexOf('/'), text.lastIndexOf('\\'));
        if (idx > 0 && (text.contains("/tmp/") || text.contains("\\tmp\\")
                || text.contains("/pdf/") || text.contains("/file/")
                || text.contains("/download/"))) {
            String[] parts = text.split("\\s+");
            String last = stripWrappingQuotes(parts[parts.length - 1]);
            if (last.contains("/") || last.contains("\\")) {
                return last;
            }
        }
        return null;
    }

    private static String stripWrappingQuotes(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String v = value.trim();
        while (v.length() >= 2
                && ((v.startsWith("\"") && v.endsWith("\""))
                || (v.startsWith("'") && v.endsWith("'")))) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private static String guessContentType(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return MediaType.TEXT_PLAIN_VALUE;
        }
        if (lower.endsWith(".json")) {
            return MediaType.APPLICATION_JSON_VALUE;
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return MediaType.TEXT_HTML_VALUE;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
