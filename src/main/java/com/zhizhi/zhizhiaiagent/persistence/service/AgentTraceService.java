package com.zhizhi.zhizhiaiagent.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.AgentTraceResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.AgentTraceStatsResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.AgentTraceEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.AgentTraceMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class AgentTraceService {

    private final AgentTraceMapper agentTraceMapper;

    @Transactional
    public String start(String userId, String chatId, String agentType) {
        String traceId = IdGenerator.nextId();
        AgentTraceEntity entity = new AgentTraceEntity();
        entity.setId(IdGenerator.nextId());
        entity.setTraceId(traceId);
        entity.setUserId(StringUtils.hasText(userId) ? userId : null);
        entity.setChatId(StringUtils.hasText(chatId) ? chatId.trim() : null);
        entity.setAgentType(agentType);
        entity.setStatus("RUNNING");
        entity.setPromptTokens(0);
        entity.setCompletionTokens(0);
        entity.setTotalTokens(0);
        entity.setStepCount(0);
        AuditHelper.fillOnCreate(entity, userId, null);
        agentTraceMapper.insert(entity);
        return traceId;
    }

    @Transactional
    public void addTokens(String traceId, Integer prompt, Integer completion) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        AgentTraceEntity entity = findByTraceId(traceId);
        if (entity == null) {
            return;
        }
        int p = prompt == null ? 0 : prompt;
        int c = completion == null ? 0 : completion;
        entity.setPromptTokens(nz(entity.getPromptTokens()) + p);
        entity.setCompletionTokens(nz(entity.getCompletionTokens()) + c);
        entity.setTotalTokens(nz(entity.getPromptTokens()) + nz(entity.getCompletionTokens()));
        AuditHelper.fillOnUpdate(entity, entity.getUserId());
        agentTraceMapper.updateById(entity);
    }

    @Transactional
    public void finish(String traceId, String status, long durationMs, int stepCount, String errorMessage) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        AgentTraceEntity entity = findByTraceId(traceId);
        if (entity == null) {
            return;
        }
        entity.setStatus(StringUtils.hasText(status) ? status : "SUCCESS");
        entity.setDurationMs(durationMs);
        entity.setStepCount(stepCount);
        if (StringUtils.hasText(errorMessage)) {
            entity.setErrorMessage(errorMessage.length() > 500
                    ? errorMessage.substring(0, 500) : errorMessage);
        }
        AuditHelper.fillOnUpdate(entity, entity.getUserId());
        agentTraceMapper.updateById(entity);
    }

    public List<AgentTraceResponse> list(String userId, String chatId, int limit) {
        int size = limit > 0 ? Math.min(limit, 100) : 30;
        LambdaQueryWrapper<AgentTraceEntity> q = new LambdaQueryWrapper<AgentTraceEntity>()
                .eq(AgentTraceEntity::getUserId, userId)
                .orderByDesc(AgentTraceEntity::getCreateDate)
                .last("LIMIT " + size);
        if (StringUtils.hasText(chatId)) {
            q.eq(AgentTraceEntity::getChatId, chatId.trim());
        }
        return agentTraceMapper.selectList(q).stream()
                .map(AgentTraceResponse::from)
                .collect(Collectors.toList());
    }

    public AgentTraceResponse get(String traceId, String userId) {
        AgentTraceEntity entity = findByTraceId(IdGenerator.requireId(traceId, "traceId"));
        if (entity == null || (userId != null && !userId.equals(entity.getUserId()))) {
            throw new IllegalArgumentException("Trace 不存在或无权访问");
        }
        return AgentTraceResponse.from(entity);
    }

    public AgentTraceStatsResponse stats(String userId) {
        List<AgentTraceEntity> list = agentTraceMapper.selectList(
                new LambdaQueryWrapper<AgentTraceEntity>()
                        .eq(AgentTraceEntity::getUserId, userId)
                        .ne(AgentTraceEntity::getStatus, "RUNNING")
                        .orderByDesc(AgentTraceEntity::getCreateDate)
                        .last("LIMIT 500"));
        long total = list.size();
        long success = list.stream().filter(e -> "SUCCESS".equals(e.getStatus())).count();
        long cancelled = list.stream().filter(e -> "CANCELLED".equals(e.getStatus())).count();
        long error = list.stream().filter(e -> "ERROR".equals(e.getStatus())).count();
        long prompt = list.stream().mapToLong(e -> nz(e.getPromptTokens())).sum();
        long completion = list.stream().mapToLong(e -> nz(e.getCompletionTokens())).sum();
        long avgDuration = list.isEmpty() ? 0
                : Math.round(list.stream().mapToLong(e -> e.getDurationMs() == null ? 0 : e.getDurationMs())
                .average().orElse(0));
        return AgentTraceStatsResponse.builder()
                .totalRuns(total)
                .successRuns(success)
                .cancelledRuns(cancelled)
                .errorRuns(error)
                .totalPromptTokens(prompt)
                .totalCompletionTokens(completion)
                .totalTokens(prompt + completion)
                .avgDurationMs(avgDuration)
                .build();
    }

    private AgentTraceEntity findByTraceId(String traceId) {
        return agentTraceMapper.selectOne(new LambdaQueryWrapper<AgentTraceEntity>()
                .eq(AgentTraceEntity::getTraceId, traceId)
                .last("LIMIT 1"));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
