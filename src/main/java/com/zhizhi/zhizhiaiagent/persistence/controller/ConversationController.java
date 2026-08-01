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

    @Operation(summary = "创建会话")
    @PostMapping
    public ApiResult<ConversationResponse> create(@RequestBody CreateConversationRequest request) {
        request.setUserId(StpUtil.getLoginIdAsString());
        request.setCreateBy(StpUtil.getLoginIdAsString());
        return ApiResult.ok(conversationService.create(request));
    }

    @Operation(summary = "会话列表（仅当前用户）")
    @GetMapping
    public ApiResult<List<ConversationResponse>> list(@RequestParam(required = false) String agentType) {
        return ApiResult.ok(conversationService.list(StpUtil.getLoginIdAsString(), agentType));
    }

    @Operation(summary = "按 chatId 查询会话")
    @GetMapping("/{chatId}")
    public ApiResult<ConversationResponse> get(@PathVariable String chatId) {
        return ApiResult.ok(conversationService.getByChatId(chatId, StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "更新会话标题/状态")
    @PutMapping("/{chatId}")
    public ApiResult<ConversationResponse> update(@PathVariable String chatId,
                                                  @RequestBody UpdateConversationRequest request) {
        return ApiResult.ok(conversationService.update(chatId, StpUtil.getLoginIdAsString(), request));
    }

    @Operation(summary = "删除会话及其消息")
    @DeleteMapping("/{chatId}")
    public ApiResult<Void> delete(@PathVariable String chatId) {
        conversationService.delete(chatId, StpUtil.getLoginIdAsString());
        return ApiResult.ok(null);
    }

    @Operation(summary = "追加消息")
    @PostMapping("/{chatId}/messages")
    public ApiResult<MessageResponse> addMessage(@PathVariable String chatId,
                                                 @RequestBody CreateMessageRequest request) {
        request.setCreateBy(StpUtil.getLoginIdAsString());
        return ApiResult.ok(conversationService.addMessage(chatId, StpUtil.getLoginIdAsString(), request));
    }

    @Operation(summary = "查询会话消息列表")
    @GetMapping("/{chatId}/messages")
    public ApiResult<List<MessageResponse>> listMessages(@PathVariable String chatId) {
        return ApiResult.ok(conversationService.listMessages(chatId, StpUtil.getLoginIdAsString()));
    }
}
