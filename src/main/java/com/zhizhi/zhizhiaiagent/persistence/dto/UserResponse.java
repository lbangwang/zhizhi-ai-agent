package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.UserEntity;
import lombok.Builder;
import lombok.Data;


import java.util.Date;

@Data
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String nickname;
    private Integer status;
    private String enterpriseId;
    private String createBy;
    private Date createDate;
    private String updateBy;
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
