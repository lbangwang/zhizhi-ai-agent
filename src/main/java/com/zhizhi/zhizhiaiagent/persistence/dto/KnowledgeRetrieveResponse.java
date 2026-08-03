package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeRetrieveResponse {
    private String query;
    private List<KnowledgeCitation> citations;
}
