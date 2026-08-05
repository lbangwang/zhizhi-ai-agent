package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 知识库文档切片列表响应。
 */
@Data
@Builder
public class KnowledgeChunksResponse {
    /** 文档 ID */
    private String documentId;
    /** 切片总数 */
    private Integer chunkCount;
    /** 是否因上限截断了返回列表 */
    private Boolean truncated;
    /** 切片条目列表 */
    private List<KnowledgeChunkItem> chunks;
}
