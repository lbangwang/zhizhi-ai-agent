package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 知识库向量检索响应。
 */
@Data
@Builder
public class KnowledgeRetrieveResponse {
    /** 检索问题 */
    private String query;
    /** 命中的引用列表 */
    private List<KnowledgeCitation> citations;
}
