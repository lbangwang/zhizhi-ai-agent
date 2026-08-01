package com.zhizhi.zhizhiaiagent.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.ConversationResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateConversationRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateMessageRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.MessageResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.UpdateConversationRequest;
import com.zhizhi.zhizhiaiagent.persistence.entity.ConversationEntity;
import com.zhizhi.zhizhiaiagent.persistence.entity.MessageEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.ConversationMapper;
import com.zhizhi.zhizhiaiagent.persistence.mapper.MessageMapper;
import com.zhizhi.zhizhiaiagent.persistence.mapper.UserMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Transactional
    public ConversationResponse create(CreateConversationRequest request) {
        String chatId = StringUtils.hasText(request.getChatId())
                ? IdGenerator.requireId(request.getChatId(), "chatId")
                : IdGenerator.nextId();
        Long exists = conversationMapper.selectCount(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getChatId, chatId));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("chatId 已存在: " + chatId);
        }
        if (!StringUtils.hasText(request.getUserId())) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String userId = IdGenerator.requireId(request.getUserId(), "userId");
        if (userMapper.selectById(userId) == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        ConversationEntity entity = new ConversationEntity();
        entity.setId(IdGenerator.nextId());
        entity.setChatId(chatId);
        entity.setUserId(userId);
        entity.setAgentType(StringUtils.hasText(request.getAgentType())
                ? request.getAgentType().trim()
                : "SUPER_AGENT");
        entity.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim()
                : "新对话");
        entity.setModel(request.getModel());
        entity.setStatus(1);
        AuditHelper.fillOnCreate(entity, request.getCreateBy(), request.getEnterpriseId());
        conversationMapper.insert(entity);
        return ConversationResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String userId, String agentType) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getUserId, userId.trim())
                .orderByDesc(ConversationEntity::getUpdateDate);
        if (StringUtils.hasText(agentType)) {
            wrapper.eq(ConversationEntity::getAgentType, agentType.trim());
        }
        return conversationMapper.selectList(wrapper).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getByChatId(String chatId, String ownerUserId) {
        return ConversationResponse.from(requireOwned(chatId, ownerUserId));
    }

    /**
     * 按 chatId 查询当前用户会话；不存在或无权时返回 null（由接口层用业务 code 表达，避免 接口 刷红）。
     */
    @Transactional(readOnly = true)
    public ConversationResponse findOwnedByChatId(String chatId, String ownerUserId) {
        ConversationEntity entity = conversationMapper.selectOne(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getChatId, chatId)
                .last("LIMIT 1"));
        if (entity == null || !Objects.equals(ownerUserId, entity.getUserId())) {
            return null;
        }
        return ConversationResponse.from(entity);
    }

    @Transactional
    public ConversationResponse update(String chatId, String ownerUserId, UpdateConversationRequest request) {
        ConversationEntity entity = requireOwned(chatId, ownerUserId);
        if (StringUtils.hasText(request.getTitle())) {
            entity.setTitle(request.getTitle().trim());
        }
        if (request.getModel() != null) {
            entity.setModel(request.getModel());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        AuditHelper.fillOnUpdate(entity, ownerUserId);
        conversationMapper.updateById(entity);
        return ConversationResponse.from(entity);
    }

    @Transactional
    public void delete(String chatId, String ownerUserId) {
        ConversationEntity entity = requireOwned(chatId, ownerUserId);
        messageMapper.delete(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getConversationId, entity.getId()));
        conversationMapper.deleteById(entity.getId());
    }

    @Transactional
    public MessageResponse addMessage(String chatId, String ownerUserId, CreateMessageRequest request) {
        if (!StringUtils.hasText(request.getRole())) {
            throw new IllegalArgumentException("role 不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content 不能为空");
        }
        ConversationEntity conversation = requireOwned(chatId, ownerUserId);

        MessageEntity message = new MessageEntity();
        message.setId(IdGenerator.nextId());
        message.setConversationId(conversation.getId());
        message.setRole(request.getRole().trim());
        message.setContent(request.getContent());
        message.setMetadata(request.getMetadata());
        String enterpriseId = StringUtils.hasText(request.getEnterpriseId())
                ? request.getEnterpriseId()
                : conversation.getEnterpriseId();
        AuditHelper.fillOnCreate(message, ownerUserId, enterpriseId);
        messageMapper.insert(message);

        AuditHelper.fillOnUpdate(conversation, ownerUserId);
        conversationMapper.updateById(conversation);
        return MessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(String chatId, String ownerUserId) {
        ConversationEntity conversation = requireOwned(chatId, ownerUserId);
        return messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversation.getId())
                        .orderByAsc(MessageEntity::getCreateDate))
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    private ConversationEntity requireOwned(String chatId, String ownerUserId) {
        ConversationEntity entity = conversationMapper.selectOne(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getChatId, chatId)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new IllegalArgumentException("会话不存在: " + chatId);
        }
        if (!Objects.equals(ownerUserId, entity.getUserId())) {
            throw new IllegalArgumentException("无权访问该会话");
        }
        return entity;
    }
}
