package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeChunksResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeDocumentResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeRetrieveRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeRetrieveResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.KnowledgeSplitPreviewResponse;
import com.zhizhi.zhizhiaiagent.persistence.service.KnowledgeDocumentService;
import com.zhizhi.zhizhiaiagent.rag.KnowledgeTextSplitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

    @Operation(summary = "切片预览（不入库，可调参）",
            description = "上传文档并预览切片结果，不写入向量库，用于调试切片策略与参数。")
    @PostMapping(value = "/documents/preview-split", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<KnowledgeSplitPreviewResponse> previewSplit(
            @Parameter(description = "待预览的文档文件", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "切片策略，如 token / paragraph", required = false, example = "token")
            @RequestParam(value = "splitStrategy", required = false) String splitStrategy,
            @Parameter(description = "按 token 切片时的目标 token 数", required = false, example = "512")
            @RequestParam(value = "chunkTokenSize", required = false) Integer chunkTokenSize,
            @Parameter(description = "按段落切片时的单段最大字符数", required = false, example = "800")
            @RequestParam(value = "paragraphMaxChars", required = false) Integer paragraphMaxChars,
            @Parameter(description = "段落切片时合并过短段的最小字符阈值", required = false, example = "200")
            @RequestParam(value = "paragraphMinMergeChars", required = false) Integer paragraphMinMergeChars,
            @Parameter(description = "低于该长度的切片不参与向量化", required = false, example = "50")
            @RequestParam(value = "minChunkLengthToEmbed", required = false) Integer minChunkLengthToEmbed,
            @Parameter(description = "最大切片数量上限", required = false, example = "200")
            @RequestParam(value = "maxNumChunks", required = false) Integer maxNumChunks) {
        KnowledgeTextSplitter.SplitParams params = KnowledgeDocumentService.buildSplitParams(
                splitStrategy,
                chunkTokenSize,
                paragraphMaxChars,
                paragraphMinMergeChars,
                minChunkLengthToEmbed,
                maxNumChunks);
        return ApiResult.ok(knowledgeDocumentService.previewSplit(file, params));
    }

    @Operation(summary = "上传文档（切片并写入 VectorStore）",
            description = "上传文档、按参数切片并写入向量库，返回文档元信息。")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<KnowledgeDocumentResponse> upload(
            @Parameter(description = "待上传的文档文件", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档标题，不传则使用文件名", required = false, example = "产品手册")
            @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "切片策略，如 token / paragraph", required = false, example = "token")
            @RequestParam(value = "splitStrategy", required = false) String splitStrategy,
            @Parameter(description = "按 token 切片时的目标 token 数", required = false, example = "512")
            @RequestParam(value = "chunkTokenSize", required = false) Integer chunkTokenSize,
            @Parameter(description = "按段落切片时的单段最大字符数", required = false, example = "800")
            @RequestParam(value = "paragraphMaxChars", required = false) Integer paragraphMaxChars,
            @Parameter(description = "段落切片时合并过短段的最小字符阈值", required = false, example = "200")
            @RequestParam(value = "paragraphMinMergeChars", required = false) Integer paragraphMinMergeChars,
            @Parameter(description = "低于该长度的切片不参与向量化", required = false, example = "50")
            @RequestParam(value = "minChunkLengthToEmbed", required = false) Integer minChunkLengthToEmbed,
            @Parameter(description = "最大切片数量上限", required = false, example = "200")
            @RequestParam(value = "maxNumChunks", required = false) Integer maxNumChunks) {
        KnowledgeTextSplitter.SplitParams params = KnowledgeDocumentService.buildSplitParams(
                splitStrategy,
                chunkTokenSize,
                paragraphMaxChars,
                paragraphMinMergeChars,
                minChunkLengthToEmbed,
                maxNumChunks);
        return ApiResult.ok(knowledgeDocumentService.upload(
                file, StpUtil.getLoginIdAsString(), title, params));
    }

    @Operation(summary = "文档列表", description = "查询当前用户已上传的知识库文档列表。")
    @GetMapping("/documents")
    public ApiResult<List<KnowledgeDocumentResponse>> list() {
        return ApiResult.ok(knowledgeDocumentService.list(StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "文档详情", description = "按文档 ID 查询单条知识库文档详情。")
    @GetMapping("/documents/{id}")
    public ApiResult<KnowledgeDocumentResponse> get(
            @Parameter(description = "知识库文档 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String id) {
        return ApiResult.ok(knowledgeDocumentService.get(id, StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "查看文档切片（已入库）", description = "查看指定文档在向量库中已入库的全部切片。")
    @GetMapping("/documents/{id}/chunks")
    public ApiResult<KnowledgeChunksResponse> listChunks(
            @Parameter(description = "知识库文档 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String id) {
        return ApiResult.ok(knowledgeDocumentService.listChunks(id, StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "删除文档（同时移除 VectorStore 切片）",
            description = "删除文档记录，并同步移除向量库中对应的切片数据。")
    @DeleteMapping("/documents/{id}")
    public ApiResult<Void> delete(
            @Parameter(description = "知识库文档 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String id) {
        knowledgeDocumentService.delete(id, StpUtil.getLoginIdAsString());
        return ApiResult.ok(null);
    }

    @Operation(summary = "相似度检索（返回引用片段）",
            description = "基于向量相似度检索知识库，返回与问题最相关的引用片段。")
    @PostMapping("/retrieve")
    public ApiResult<KnowledgeRetrieveResponse> retrieve(@RequestBody KnowledgeRetrieveRequest request) {
        return ApiResult.ok(knowledgeDocumentService.retrieve(
                StpUtil.getLoginIdAsString(),
                request.getQuery(),
                request.getTopK()));
    }
}
