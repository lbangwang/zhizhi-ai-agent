package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.UserEntity;
import lombok.Builder;
import lombok.Data;


import java.util.Date;

/**
 * 用户响应 DTO（不含密码等敏感字段）。
 */
@Data
@Builder
public class UserResponse {
    /** 主键 ID */
    private String id;
    /** 登录用户名 */
    private String username;
    /** 用户昵称 */
    private String nickname;
    /** 1=正常 0=禁用 */
    private Integer status;
    /** 企业/租户 ID */
    private String enterpriseId;
    /** 创建人标识 */
    private String createBy;
    /** 创建时间 */
    private Date createDate;
    /** 最后更新人标识 */
    private String updateBy;
    /** 最后更新时间 */
    private Date updateDate;

    public static UserResponse from(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(entity.getNickname())
                .status(entity.getStatus())
                .enterpriseId(entity.getEnterpriseId())
                .createBy(entity.getCreateBy())
                .createDate(entity.getCreateDate())
                .updateBy(entity.getUpdateBy())
                .updateDate(entity.getUpdateDate())
                .build();
    }
}
