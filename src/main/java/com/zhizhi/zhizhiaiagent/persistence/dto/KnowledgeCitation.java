package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 知识库检索引用条目。
 */
@Data
@Builder
public class KnowledgeCitation {
    /** 来源文档 ID */
    private String documentId;
    /** 向量切片 ID */
    private String chunkId;
    /** 原始文件名 */
    private String filename;
    /** 文档标题 */
    private String title;
    /** 命中片段摘要 */
    private String snippet;
    /** 相似度得分 */
    private Double score;
}
