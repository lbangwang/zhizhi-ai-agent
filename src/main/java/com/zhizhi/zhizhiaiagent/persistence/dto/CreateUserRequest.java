package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String nickname;
}
