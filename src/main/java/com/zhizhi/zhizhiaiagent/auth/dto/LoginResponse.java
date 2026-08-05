package com.zhizhi.zhizhiaiagent.auth.dto;

import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /** 访问令牌 */
    private String token;
    /** 令牌在请求头中的字段名 */
    private String tokenName;
    /** 令牌前缀，如 Bearer */
    private String tokenPrefix;
    /** 当前登录用户信息 */
    private UserResponse user;
}
