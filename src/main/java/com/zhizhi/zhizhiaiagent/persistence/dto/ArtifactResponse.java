package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ArtifactEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ArtifactResponse {
    private String id;
    private String userId;
    private String chatId;
    private String toolName;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Date createDate;
    private Date updateDate;

    public static ArtifactResponse from(ArtifactEntity entity) {
        return ArtifactResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .chatId(entity.getChatId())
                .toolName(entity.getToolName())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .createDate(entity.getCreateDate())
                .updateDate(entity.getUpdateDate())
                .build();
    }
}
