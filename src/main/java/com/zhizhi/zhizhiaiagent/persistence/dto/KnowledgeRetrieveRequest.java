package com.zhizhi.zhizhiaiagent.persistence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库向量检索请求。
 */
@Data
@Schema(description = "知识库向量检索请求")
public class KnowledgeRetrieveRequest {

    @Schema(description = "检索问题", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "产品的退货政策是什么？")
    private String query;

    @Schema(description = "返回条数，默认取配置", example = "5")
    private Integer topK;
}
