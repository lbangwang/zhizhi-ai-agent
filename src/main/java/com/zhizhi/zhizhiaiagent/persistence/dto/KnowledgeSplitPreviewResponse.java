package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档切分预览响应。
 */
@Data
@Builder
public class KnowledgeSplitPreviewResponse {
    /** 原始文件名 */
    private String filename;
    /** 提取出的文本字符数 */
    private Integer extractedCharCount;
    /** 切分策略名称 */
    private String strategy;
    /** 本次实际生效的切分参数 */
    private Map<String, Object> params;
    /** 切片总数 */
    private Integer chunkCount;
    /** 是否因上限截断了返回列表（总数仍以 chunkCount 为准） */
    private Boolean truncated;
    /** 预览切片条目列表 */
    private List<KnowledgeChunkItem> chunks;
}
