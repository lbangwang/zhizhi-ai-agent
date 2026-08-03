package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KnowledgeDocumentEntity extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;

    private String title;

    private String filename;

    private String contentType;

    private String filePath;

    private Integer chunkCount;

    /** VectorStore 中的 chunk Document.id 列表（JSON 数组） */
    private String chunkIds;

    /** 1=就绪 0=失败 */
    private Integer status;

    private String errorMessage;
}
