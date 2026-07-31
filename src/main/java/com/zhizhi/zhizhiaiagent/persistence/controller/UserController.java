package com.zhizhi.zhizhiaiagent.persistence.controller;

import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateUserRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import com.zhizhi.zhizhiaiagent.persistence.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "创建用户（D4 鉴权前临时接口）")
    @PostMapping
    public ApiResult<UserResponse> create(@RequestBody CreateUserRequest request) {
        return ApiResult.ok(userService.create(request));
    }

    @Operation(summary = "按 ID 查询用户")
    @GetMapping("/{id}")
    public ApiResult<UserResponse> get(@PathVariable Long id) {
        return ApiResult.ok(userService.getById(id));
    }
}
