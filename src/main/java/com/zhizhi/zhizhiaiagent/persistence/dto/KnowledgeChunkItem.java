package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 知识库文档切片条目。
 */
@Data
@Builder
public class KnowledgeChunkItem {
    /** 切片序号（从 0 起） */
    private Integer index;
    /** VectorStore 中的 ID；预览阶段可为空 */
    private String chunkId;
    /** 切片字符数 */
    private Integer charCount;
    /** 切片文本内容 */
    private String text;
}
