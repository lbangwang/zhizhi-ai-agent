package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("artifact")
public class ArtifactEntity extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;

    private String chatId;

    private String toolName;

    private String fileName;

    private String contentType;

    private String filePath;

    private Long fileSize;

    private String sourcePath;
}
