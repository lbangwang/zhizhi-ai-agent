package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.KnowledgeDocumentEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 知识库文档响应 DTO。
 */
@Data
@Builder
public class KnowledgeDocumentResponse {
    /** 主键 ID */
    private String id;
    /** 上传用户 ID */
    private String userId;
    /** 文档标题 */
    private String title;
    /** 原始文件名 */
    private String filename;
    /** MIME 类型 */
    private String contentType;
    /** 切片数量 */
    private Integer chunkCount;
    /** 1=就绪 0=失败 */
    private Integer status;
    /** 处理失败时的错误信息 */
    private String errorMessage;
    /** 创建时间 */
    private Date createDate;
    /** 最后更新时间 */
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
