package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ArtifactEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Agent 工具产物响应 DTO。
 */
@Data
@Builder
public class ArtifactResponse {
    /** 主键 ID */
    private String id;
    /** 所属用户 ID */
    private String userId;
    /** 关联会话 chatId */
    private String chatId;
    /** 产生该产物的工具名称 */
    private String toolName;
    /** 产物文件名 */
    private String fileName;
    /** MIME 类型 */
    private String contentType;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 创建时间 */
    private Date createDate;
    /** 最后更新时间 */
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
