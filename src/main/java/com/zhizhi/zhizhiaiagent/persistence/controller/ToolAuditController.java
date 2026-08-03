package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.ToolAuditLogResponse;
import com.zhizhi.zhizhiaiagent.persistence.service.ToolAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "工具审计")
@RestController
@RequestMapping("/tool-audits")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class ToolAuditController {

    private final ToolAuditService toolAuditService;

    @Operation(summary = "工具调用审计列表（按会话筛选）")
    @GetMapping
    public ApiResult<List<ToolAuditLogResponse>> list(
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResult.ok(toolAuditService.listByChatId(
                StpUtil.getLoginIdAsString(), chatId, limit));
    }
}
