package com.zhizhi.zhizhiaiagent.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.ToolAuditLogResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.ToolAuditLogEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.ToolAuditLogMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class ToolAuditService {

    private static final int ARGS_MAX = 1000;
    private static final int RESULT_MAX = 2000;

    private final ToolAuditLogMapper toolAuditLogMapper;

    @Transactional
    public void record(
            String userId,
            String chatId,
            String toolName,
            String arguments,
            String result,
            long durationMs) {
        if (!StringUtils.hasText(toolName)) {
            return;
        }
        ToolAuditLogEntity entity = new ToolAuditLogEntity();
        entity.setId(IdGenerator.nextId());
        entity.setUserId(StringUtils.hasText(userId) ? userId.trim() : null);
        entity.setChatId(StringUtils.hasText(chatId) ? chatId.trim() : null);
        entity.setToolName(toolName.trim());
        entity.setArgumentsSummary(truncate(arguments, ARGS_MAX));
        entity.setResultSummary(truncate(result, RESULT_MAX));
        entity.setSuccess(isSuccess(result) ? 1 : 0);
        entity.setDurationMs(durationMs);
        AuditHelper.fillOnCreate(entity, entity.getUserId(), null);
        toolAuditLogMapper.insert(entity);
        log.debug("tool audit saved tool={}, success={}", toolName, entity.getSuccess());
    }

    public List<ToolAuditLogResponse> listByChatId(String userId, String chatId, int limit) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        int size = limit > 0 ? Math.min(limit, 200) : 50;
        LambdaQueryWrapper<ToolAuditLogEntity> q = new LambdaQueryWrapper<ToolAuditLogEntity>()
                .eq(ToolAuditLogEntity::getUserId, userId)
                .orderByDesc(ToolAuditLogEntity::getCreateDate)
                .last("LIMIT " + size);
        if (StringUtils.hasText(chatId)) {
            q.eq(ToolAuditLogEntity::getChatId, chatId.trim());
        }
        return toolAuditLogMapper.selectList(q).stream()
                .map(ToolAuditLogResponse::from)
                .collect(Collectors.toList());
    }

    private static boolean isSuccess(String result) {
        if (!StringUtils.hasText(result)) {
            return true;
        }
        String lower = result.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("error")
                || lower.contains("exception")
                || lower.contains("not recognized")
                || lower.contains("failed")) {
            return false;
        }
        return true;
    }

    private static String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
