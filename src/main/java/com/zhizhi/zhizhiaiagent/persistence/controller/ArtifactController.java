package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.ArtifactResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.ArtifactEntity;
import com.zhizhi.zhizhiaiagent.persistence.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "产物")
@RestController
@RequestMapping("/artifacts")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class ArtifactController {

    private final ArtifactService artifactService;

    @Operation(summary = "当前会话/用户产物列表",
            description = "查询 Agent 生成的产物（文件、报告等）列表，可按会话筛选。")
    @GetMapping
    public ApiResult<List<ArtifactResponse>> list(
            @Parameter(description = "会话 ID，不传则返回当前用户全部产物", required = false,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            @RequestParam(value = "chatId", required = false) String chatId) {
        return ApiResult.ok(artifactService.listByChatId(StpUtil.getLoginIdAsString(), chatId));
    }

    @Operation(summary = "下载产物文件", description = "按产物 ID 下载 Agent 生成的文件。")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @Parameter(description = "产物 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        ArtifactEntity entity = artifactService.requireOwned(id, userId);
        Resource resource = artifactService.loadAsResource(id, userId);
        String encoded = URLEncoder.encode(entity.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(artifactService.resolveContentType(entity)))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
