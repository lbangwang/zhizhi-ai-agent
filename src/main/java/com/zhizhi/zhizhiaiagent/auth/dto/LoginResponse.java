package com.zhizhi.zhizhiaiagent.auth.dto;

import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenName;
    private String tokenPrefix;
    private UserResponse user;
}
