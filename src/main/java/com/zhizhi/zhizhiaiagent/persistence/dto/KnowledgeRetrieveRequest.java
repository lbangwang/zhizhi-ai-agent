package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class KnowledgeRetrieveRequest {
    /** 检索问题 */
    private String query;
    /** 返回条数，默认取配置 */
    private Integer topK;
}
