package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.ToolAuditLogResponse;
import com.zhizhi.zhizhiaiagent.persistence.service.ToolAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "工具调用审计列表（按会话筛选）",
            description = "查询 Agent 工具调用的审计日志，可按会话 ID 筛选。")
    @GetMapping
    public ApiResult<List<ToolAuditLogResponse>> list(
            @Parameter(description = "会话 ID，不传则返回当前用户全部审计记录", required = false,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            @RequestParam(value = "chatId", required = false) String chatId,
            @Parameter(description = "返回条数上限", required = false, example = "50")
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResult.ok(toolAuditService.listByChatId(
                StpUtil.getLoginIdAsString(), chatId, limit));
    }
}
