package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.dto.ConversationResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateConversationRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateMessageRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.MessageResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.UpdateConversationRequest;
import com.zhizhi.zhizhiaiagent.persistence.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "会话与消息")
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "创建会话", description = "创建新的对话会话，userId 与 createBy 由服务端自动填充。")
    @PostMapping
    public ApiResult<ConversationResponse> create(@RequestBody CreateConversationRequest request) {
        request.setUserId(StpUtil.getLoginIdAsString());
        request.setCreateBy(StpUtil.getLoginIdAsString());
        return ApiResult.ok(conversationService.create(request));
    }

    @Operation(summary = "会话列表（仅当前用户）", description = "查询当前登录用户的会话列表，可按智能体类型筛选。")
    @GetMapping
    public ApiResult<List<ConversationResponse>> list(
            @Parameter(description = "智能体类型，如 SUPER_AGENT / MULTI_AGENT", required = false,
                    example = "SUPER_AGENT")
            @RequestParam(required = false) String agentType) {
        return ApiResult.ok(conversationService.list(StpUtil.getLoginIdAsString(), agentType));
    }

    @Operation(summary = "按 chatId 查询会话", description = "根据会话 ID 查询单条会话详情。")
    @GetMapping("/{chatId}")
    public ApiResult<ConversationResponse> get(
            @Parameter(description = "会话 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String chatId) {
        ConversationResponse conversation = conversationService.findOwnedByChatId(
                chatId, StpUtil.getLoginIdAsString());
        if (conversation == null) {
            // HTTP 200 + 业务失败码，前端用 code/message 分支，避免接口报红
            return ApiResult.fail("会话不存在");
        }
        return ApiResult.ok(conversation);
    }

    @Operation(summary = "更新会话标题/状态", description = "更新会话的标题、模型或归档状态。")
    @PutMapping("/{chatId}")
    public ApiResult<ConversationResponse> update(
            @Parameter(description = "会话 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String chatId,
            @RequestBody UpdateConversationRequest request) {
        return ApiResult.ok(conversationService.update(chatId, StpUtil.getLoginIdAsString(), request));
    }

    @Operation(summary = "删除会话及其消息", description = "删除指定会话及其全部消息记录。")
    @DeleteMapping("/{chatId}")
    public ApiResult<Void> delete(
            @Parameter(description = "会话 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String chatId) {
        conversationService.delete(chatId, StpUtil.getLoginIdAsString());
        return ApiResult.ok(null);
    }

    @Operation(summary = "追加消息", description = "向指定会话追加一条消息，createBy 由服务端自动填充。")
    @PostMapping("/{chatId}/messages")
    public ApiResult<MessageResponse> addMessage(
            @Parameter(description = "会话 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String chatId,
            @RequestBody CreateMessageRequest request) {
        request.setCreateBy(StpUtil.getLoginIdAsString());
        return ApiResult.ok(conversationService.addMessage(chatId, StpUtil.getLoginIdAsString(), request));
    }

    @Operation(summary = "查询会话消息列表", description = "查询指定会话下的全部消息，按时间顺序返回。")
    @GetMapping("/{chatId}/messages")
    public ApiResult<List<MessageResponse>> listMessages(
            @Parameter(description = "会话 ID", required = true, example = "a1b2c3d4e5f6789012345678abcdef01")
            @PathVariable String chatId) {
        return ApiResult.ok(conversationService.listMessages(chatId, StpUtil.getLoginIdAsString()));
    }
}
