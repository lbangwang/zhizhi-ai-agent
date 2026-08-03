package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeCitation {
    private String documentId;
    private String chunkId;
    private String filename;
    private String title;
    private String snippet;
    private Double score;
}
