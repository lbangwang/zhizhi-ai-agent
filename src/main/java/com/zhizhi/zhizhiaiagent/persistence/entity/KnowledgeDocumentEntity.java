package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档实体，对应 kb_document 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KnowledgeDocumentEntity extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 上传用户 ID */
    private String userId;

    /** 文档标题 */
    private String title;

    /** 原始文件名 */
    private String filename;

    /** MIME 类型 */
    private String contentType;

    /** 本地存储路径 */
    private String filePath;

    /** 切片数量 */
    private Integer chunkCount;

    /** VectorStore 中的 chunk Document.id 列表（JSON 数组） */
    private String chunkIds;

    /** 1=就绪 0=失败 */
    private Integer status;

    /** 处理失败时的错误信息 */
    private String errorMessage;
}
