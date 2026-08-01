package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 表公共字段：审计 + 租户 + 逻辑删除。
 */
@Data
public abstract class BaseEntity {

    private Date createDate;

    private String createBy;

    private Date updateDate;

    private String updateBy;

    /** 0=未删除 1=已删除 */
    @TableLogic
    private Integer isDel;

    /** 企业/租户 ID */
    private String enterpriseId;
}
