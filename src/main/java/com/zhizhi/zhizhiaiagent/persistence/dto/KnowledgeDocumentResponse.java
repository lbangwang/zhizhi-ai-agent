package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.KnowledgeDocumentEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class KnowledgeDocumentResponse {
    private String id;
    private String userId;
    private String title;
    private String filename;
    private String contentType;
    private Integer chunkCount;
    private Integer status;
    private String errorMessage;
    private Date createDate;
    private Date updateDate;

    public static KnowledgeDocumentResponse from(KnowledgeDocumentEntity entity) {
        return KnowledgeDocumentResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .filename(entity.getFilename())
                .contentType(entity.getContentType())
                .chunkCount(entity.getChunkCount())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .createDate(entity.getCreateDate())
                .updateDate(entity.getUpdateDate())
                .build();
    }
}
