package com.zhizhi.zhizhiaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * W2：知识库切片。
 * <ul>
 *   <li>{@code token}：按 token 窗口切（Spring AI TokenTextSplitter）</li>
 *   <li>{@code paragraph}：优先按空行分段，过短合并、过长再回退 token 切</li>
 * </ul>
 */
@Slf4j
@Component
public class KnowledgeTextSplitter {

    public enum Strategy {
        TOKEN,
        PARAGRAPH;

        static Strategy from(String raw) {
            if (!StringUtils.hasText(raw)) {
                return PARAGRAPH;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "token" -> TOKEN;
                case "paragraph", "para", "section" -> PARAGRAPH;
                default -> {
                    log.warn("Unknown split-strategy '{}', fallback to paragraph", raw);
                    yield PARAGRAPH;
                }
            };
        }
    }

    /** 切片策略：token | paragraph */
    private final Strategy strategy;

    /** token 策略：每片目标 token 数；paragraph 策略：超长段回退 token 切时复用 */
    private final int defaultChunkSize;

    /** token 策略：标点软截断的最小字符数 */
    private final int minChunkSizeChars;

    /** 过短片段丢弃阈值（字符） */
    private final int minChunkLengthToEmbed;

    /** 单篇最多切片数 */
    private final int maxNumChunks;

    /**
     * paragraph 策略：单段（或合并后）超过该字符数则回退 TokenTextSplitter。
     * 未配置时默认约为 {@code chunk-token-size * 2}（中文粗略换算）。
     */
    private final int paragraphMaxChars;

    /**
     * paragraph 策略：短于该字符数的段会与后续段合并，减少碎片。
     */
    private final int paragraphMinMergeChars;

    public KnowledgeTextSplitter(
            @Value("${app.knowledge.split-strategy:paragraph}") String splitStrategy,
            @Value("${app.knowledge.chunk-token-size:400}") int defaultChunkSize,
            @Value("${app.knowledge.min-chunk-size-chars:100}") int minChunkSizeChars,
            @Value("${app.knowledge.min-chunk-length-to-embed:10}") int minChunkLengthToEmbed,
            @Value("${app.knowledge.max-num-chunks:5000}") int maxNumChunks,
            @Value("${app.knowledge.paragraph-max-chars:0}") int paragraphMaxChars,
            @Value("${app.knowledge.paragraph-min-merge-chars:80}") int paragraphMinMergeChars) {
        this.strategy = Strategy.from(splitStrategy);
        this.defaultChunkSize = defaultChunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.paragraphMaxChars = paragraphMaxChars > 0
                ? paragraphMaxChars
                : Math.max(400, defaultChunkSize * 2);
        this.paragraphMinMergeChars = Math.max(0, paragraphMinMergeChars);
        log.info("KnowledgeTextSplitter ready, strategy={}, chunkTokenSize={}, paragraphMaxChars={}",
                this.strategy, this.defaultChunkSize, this.paragraphMaxChars);
    }

    public List<Document> split(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return switch (strategy) {
            case TOKEN -> splitByToken(documents);
            case PARAGRAPH -> splitByParagraph(documents);
        };
    }

    /** 纯 TokenTextSplitter */
    private List<Document> splitByToken(List<Document> documents) {
        return tokenSplitter().apply(documents);
    }

    /**
     * 按段切：
     * <ol>
     *   <li>用空行拆成自然段</li>
     *   <li>过短段与下一段合并</li>
     *   <li>合并后仍超长 → 对该段单独做 token 切</li>
     * </ol>
     */
    private List<Document> splitByParagraph(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document source : documents) {
            if (source == null || !StringUtils.hasText(source.getText())) {
                continue;
            }
            Map<String, Object> baseMeta = copyMeta(source);
            List<String> paragraphs = splitParagraphs(source.getText());
            List<String> merged = mergeShortParagraphs(paragraphs);

            for (String block : merged) {
                if (!StringUtils.hasText(block)) {
                    continue;
                }
                String trimmed = block.trim();
                if (trimmed.length() < minChunkLengthToEmbed) {
                    continue;
                }
                if (trimmed.length() <= paragraphMaxChars) {
                    result.add(new Document(trimmed, new HashMap<>(baseMeta)));
                    if (result.size() >= maxNumChunks) {
                        return result;
                    }
                } else {
                    // 超长段：回退 token 切，避免单片过大
                    Document longDoc = new Document(trimmed, new HashMap<>(baseMeta));
                    List<Document> parts = tokenSplitter().apply(List.of(longDoc));
                    for (Document part : parts) {
                        if (part == null || !StringUtils.hasText(part.getText())) {
                            continue;
                        }
                        if (part.getText().trim().length() < minChunkLengthToEmbed) {
                            continue;
                        }
                        result.add(part);
                        if (result.size() >= maxNumChunks) {
                            return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    private TokenTextSplitter tokenSplitter() {
        return new TokenTextSplitter(
                defaultChunkSize,
                minChunkSizeChars,
                minChunkLengthToEmbed,
                maxNumChunks,
                true);
    }

    /** 按空行分段（兼容 \r\n） */
    static List<String> splitParagraphs(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\\n\\s*\\n+");
        List<String> paragraphs = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                paragraphs.add(part.trim());
            }
        }
        // 没有空行时整篇当作一段，交给后续超长回退
        if (paragraphs.isEmpty() && StringUtils.hasText(normalized)) {
            paragraphs.add(normalized.trim());
        }
        return paragraphs;
    }

    /** 短段向后合并，直到达到 paragraphMinMergeChars 或没有下一段 */
    private List<String> mergeShortParagraphs(List<String> paragraphs) {
        if (paragraphs.isEmpty() || paragraphMinMergeChars <= 0) {
            return paragraphs;
        }
        List<String> merged = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String p : paragraphs) {
            if (buf.isEmpty()) {
                buf.append(p);
                continue;
            }
            if (buf.length() < paragraphMinMergeChars) {
                buf.append("\n\n").append(p);
            } else {
                merged.add(buf.toString());
                buf.setLength(0);
                buf.append(p);
            }
        }
        if (!buf.isEmpty()) {
            // 最后一段过短则并入上一段
            if (buf.length() < paragraphMinMergeChars && !merged.isEmpty()) {
                int last = merged.size() - 1;
                merged.set(last, merged.get(last) + "\n\n" + buf);
            } else {
                merged.add(buf.toString());
            }
        }
        return merged;
    }

    private static Map<String, Object> copyMeta(Document source) {
        return source.getMetadata() == null
                ? new HashMap<>()
                : new HashMap<>(source.getMetadata());
    }
}
