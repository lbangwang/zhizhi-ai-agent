package com.zhizhi.zhizhiaiagent.rag;

import lombok.Builder;
import lombok.Getter;
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
 * 支持请求级 {@link SplitParams} 覆盖 YAML 默认值（用于预览调参与入库一致）。
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

        public String wireName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * 单次切分参数；字段为 null 时回退到 YAML 默认。
     */
    @Getter
    @Builder
    public static class SplitParams {
        /** token | paragraph */
        String strategy;
        Integer chunkTokenSize;
        Integer minChunkSizeChars;
        Integer minChunkLengthToEmbed;
        Integer maxNumChunks;
        Integer paragraphMaxChars;
        Integer paragraphMinMergeChars;
    }

    /** 解析后的有效参数（含默认值展开） */
    @Getter
    @Builder
    public static class ResolvedSplitParams {
        Strategy strategy;
        int chunkTokenSize;
        int minChunkSizeChars;
        int minChunkLengthToEmbed;
        int maxNumChunks;
        int paragraphMaxChars;
        int paragraphMinMergeChars;
    }

    private final SplitParams defaults;

    public KnowledgeTextSplitter(
            @Value("${app.knowledge.split-strategy:paragraph}") String splitStrategy,
            @Value("${app.knowledge.chunk-token-size:400}") int defaultChunkSize,
            @Value("${app.knowledge.min-chunk-size-chars:100}") int minChunkSizeChars,
            @Value("${app.knowledge.min-chunk-length-to-embed:10}") int minChunkLengthToEmbed,
            @Value("${app.knowledge.max-num-chunks:5000}") int maxNumChunks,
            @Value("${app.knowledge.paragraph-max-chars:0}") int paragraphMaxChars,
            @Value("${app.knowledge.paragraph-min-merge-chars:80}") int paragraphMinMergeChars) {
        int resolvedParagraphMax = paragraphMaxChars > 0
                ? paragraphMaxChars
                : Math.max(400, defaultChunkSize * 2);
        this.defaults = SplitParams.builder()
                .strategy(splitStrategy)
                .chunkTokenSize(defaultChunkSize)
                .minChunkSizeChars(minChunkSizeChars)
                .minChunkLengthToEmbed(minChunkLengthToEmbed)
                .maxNumChunks(maxNumChunks)
                .paragraphMaxChars(resolvedParagraphMax)
                .paragraphMinMergeChars(Math.max(0, paragraphMinMergeChars))
                .build();
        log.info("KnowledgeTextSplitter ready, strategy={}, chunkTokenSize={}, paragraphMaxChars={}",
                Strategy.from(splitStrategy), defaultChunkSize, resolvedParagraphMax);
    }

    public SplitParams getDefaults() {
        return defaults;
    }

    public ResolvedSplitParams resolve(SplitParams override) {
        SplitParams o = override == null ? SplitParams.builder().build() : override;
        int chunkTokenSize = positiveOr(o.getChunkTokenSize(), defaults.getChunkTokenSize());
        int paragraphMaxChars = o.getParagraphMaxChars() != null && o.getParagraphMaxChars() > 0
                ? o.getParagraphMaxChars()
                : (defaults.getParagraphMaxChars() != null
                ? defaults.getParagraphMaxChars()
                : Math.max(400, chunkTokenSize * 2));
        return ResolvedSplitParams.builder()
                .strategy(Strategy.from(StringUtils.hasText(o.getStrategy())
                        ? o.getStrategy()
                        : defaults.getStrategy()))
                .chunkTokenSize(chunkTokenSize)
                .minChunkSizeChars(positiveOr(o.getMinChunkSizeChars(), defaults.getMinChunkSizeChars()))
                .minChunkLengthToEmbed(positiveOr(o.getMinChunkLengthToEmbed(), defaults.getMinChunkLengthToEmbed()))
                .maxNumChunks(positiveOr(o.getMaxNumChunks(), defaults.getMaxNumChunks()))
                .paragraphMaxChars(paragraphMaxChars)
                .paragraphMinMergeChars(o.getParagraphMinMergeChars() != null
                        ? Math.max(0, o.getParagraphMinMergeChars())
                        : Math.max(0, defaults.getParagraphMinMergeChars() == null
                        ? 0
                        : defaults.getParagraphMinMergeChars()))
                .build();
    }

    /** 使用 YAML 默认参数切分 */
    public List<Document> split(List<Document> documents) {
        return split(documents, null);
    }

    /** 使用可选覆盖参数切分 */
    public List<Document> split(List<Document> documents, SplitParams override) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        ResolvedSplitParams params = resolve(override);
        return switch (params.getStrategy()) {
            case TOKEN -> splitByToken(documents, params);
            case PARAGRAPH -> splitByParagraph(documents, params);
        };
    }

    private List<Document> splitByToken(List<Document> documents, ResolvedSplitParams params) {
        return tokenSplitter(params).apply(documents);
    }

    private List<Document> splitByParagraph(List<Document> documents, ResolvedSplitParams params) {
        List<Document> result = new ArrayList<>();
        for (Document source : documents) {
            if (source == null || !StringUtils.hasText(source.getText())) {
                continue;
            }
            Map<String, Object> baseMeta = copyMeta(source);
            List<String> paragraphs = splitParagraphs(source.getText());
            List<String> merged = mergeShortParagraphs(paragraphs, params.getParagraphMinMergeChars());

            for (String block : merged) {
                if (!StringUtils.hasText(block)) {
                    continue;
                }
                String trimmed = block.trim();
                if (trimmed.length() < params.getMinChunkLengthToEmbed()) {
                    continue;
                }
                if (trimmed.length() <= params.getParagraphMaxChars()) {
                    result.add(new Document(trimmed, new HashMap<>(baseMeta)));
                    if (result.size() >= params.getMaxNumChunks()) {
                        return result;
                    }
                } else {
                    Document longDoc = new Document(trimmed, new HashMap<>(baseMeta));
                    List<Document> parts = tokenSplitter(params).apply(List.of(longDoc));
                    for (Document part : parts) {
                        if (part == null || !StringUtils.hasText(part.getText())) {
                            continue;
                        }
                        if (part.getText().trim().length() < params.getMinChunkLengthToEmbed()) {
                            continue;
                        }
                        result.add(part);
                        if (result.size() >= params.getMaxNumChunks()) {
                            return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    private TokenTextSplitter tokenSplitter(ResolvedSplitParams params) {
        return new TokenTextSplitter(
                params.getChunkTokenSize(),
                params.getMinChunkSizeChars(),
                params.getMinChunkLengthToEmbed(),
                params.getMaxNumChunks(),
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
        if (paragraphs.isEmpty() && StringUtils.hasText(normalized)) {
            paragraphs.add(normalized.trim());
        }
        return paragraphs;
    }

    static List<String> mergeShortParagraphs(List<String> paragraphs, int paragraphMinMergeChars) {
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
            if (buf.length() < paragraphMinMergeChars && !merged.isEmpty()) {
                int last = merged.size() - 1;
                merged.set(last, merged.get(last) + "\n\n" + buf);
            } else {
                merged.add(buf.toString());
            }
        }
        return merged;
    }

    private static int positiveOr(Integer value, Integer fallback) {
        if (value != null && value > 0) {
            return value;
        }
        return fallback == null || fallback <= 0 ? 1 : fallback;
    }

    private static Map<String, Object> copyMeta(Document source) {
        return source.getMetadata() == null
                ? new HashMap<>()
                : new HashMap<>(source.getMetadata());
    }
}
