package com.zhizhi.zhizhiaiagent.auth.controller;

import com.zhizhi.zhizhiaiagent.auth.dto.LoginRequest;
import com.zhizhi.zhizhiaiagent.auth.dto.LoginResponse;
import com.zhizhi.zhizhiaiagent.auth.dto.RegisterRequest;
import com.zhizhi.zhizhiaiagent.auth.service.AuthService;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class AuthController {

    @Autowired
    private  AuthService authService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok(null);
    }

    @Operation(summary = "当前用户")
    @GetMapping("/me")
    public ApiResult<UserResponse> me() {
        return ApiResult.ok(authService.currentUser());
    }
}
