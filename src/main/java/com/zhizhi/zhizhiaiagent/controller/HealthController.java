package com.zhizhi.zhizhiaiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Operation(summary = "服务健康检查", description = "返回 ok 表示服务正常运行。")
    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}
