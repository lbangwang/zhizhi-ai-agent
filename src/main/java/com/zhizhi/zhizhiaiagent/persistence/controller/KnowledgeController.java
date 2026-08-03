package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeDocumentResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeRetrieveRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeRetrieveResponse;
import com.zhizhi.zhizhiaiagent.persistence.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "知识库")
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class KnowledgeController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @Operation(summary = "上传文档（切片并写入 VectorStore）")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<KnowledgeDocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        return ApiResult.ok(knowledgeDocumentService.upload(file, StpUtil.getLoginIdAsString(), title));
    }

    @Operation(summary = "文档列表")
    @GetMapping("/documents")
    public ApiResult<List<KnowledgeDocumentResponse>> list() {
        return ApiResult.ok(knowledgeDocumentService.list(StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "文档详情")
    @GetMapping("/documents/{id}")
    public ApiResult<KnowledgeDocumentResponse> get(@PathVariable String id) {
        return ApiResult.ok(knowledgeDocumentService.get(id, StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "删除文档（同时移除 VectorStore 切片）")
    @DeleteMapping("/documents/{id}")
    public ApiResult<Void> delete(@PathVariable String id) {
        knowledgeDocumentService.delete(id, StpUtil.getLoginIdAsString());
        return ApiResult.ok(null);
    }

    @Operation(summary = "相似度检索（返回引用片段）")
    @PostMapping("/retrieve")
    public ApiResult<KnowledgeRetrieveResponse> retrieve(@RequestBody KnowledgeRetrieveRequest request) {
        return ApiResult.ok(knowledgeDocumentService.retrieve(
                StpUtil.getLoginIdAsString(),
                request.getQuery(),
                request.getTopK()));
    }
}
