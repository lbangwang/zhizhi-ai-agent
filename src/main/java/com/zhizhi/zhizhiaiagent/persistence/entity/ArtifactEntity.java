package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 工具产物实体，对应 artifact 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("artifact")
public class ArtifactEntity extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
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

    /** 本地存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 工具侧原始路径或标识 */
    private String sourcePath;
}
