package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String nickname;
    /** 32 位字符串，可选，无默认值 */
    private String enterpriseId;
    private String createBy;
}
