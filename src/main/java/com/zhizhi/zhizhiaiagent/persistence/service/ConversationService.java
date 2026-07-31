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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Transactional
    public ConversationResponse create(CreateConversationRequest request) {
        String chatId = StringUtils.hasText(request.getChatId())
                ? request.getChatId().trim()
                : "chat_" + UUID.randomUUID().toString().replace("-", "");
        Long exists = conversationMapper.selectCount(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getChatId, chatId));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("chatId 已存在: " + chatId);
        }
        if (request.getUserId() != null && userMapper.selectById(request.getUserId()) == null) {
            throw new IllegalArgumentException("用户不存在: " + request.getUserId());
        }

        LocalDateTime now = LocalDateTime.now();
        ConversationEntity entity = new ConversationEntity();
        entity.setChatId(chatId);
        entity.setUserId(request.getUserId());
        entity.setAgentType(StringUtils.hasText(request.getAgentType())
                ? request.getAgentType().trim()
                : "SUPER_AGENT");
        entity.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim()
                : "新对话");
        entity.setModel(request.getModel());
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        conversationMapper.insert(entity);
        return ConversationResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(Long userId) {
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<ConversationEntity>()
                .orderByDesc(ConversationEntity::getUpdatedAt);
        if (userId != null) {
            wrapper.eq(ConversationEntity::getUserId, userId);
        }
        return conversationMapper.selectList(wrapper).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getByChatId(String chatId) {
        return ConversationResponse.from(requireByChatId(chatId));
    }

    @Transactional
    public ConversationResponse update(String chatId, UpdateConversationRequest request) {
        ConversationEntity entity = requireByChatId(chatId);
        if (StringUtils.hasText(request.getTitle())) {
            entity.setTitle(request.getTitle().trim());
        }
        if (request.getModel() != null) {
            entity.setModel(request.getModel());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(entity);
        return ConversationResponse.from(entity);
    }

    @Transactional
    public void delete(String chatId) {
        ConversationEntity entity = requireByChatId(chatId);
        messageMapper.delete(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getConversationId, entity.getId()));
        conversationMapper.deleteById(entity.getId());
    }

    @Transactional
    public MessageResponse addMessage(String chatId, CreateMessageRequest request) {
        if (!StringUtils.hasText(request.getRole())) {
            throw new IllegalArgumentException("role 不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content 不能为空");
        }
        ConversationEntity conversation = requireByChatId(chatId);
        LocalDateTime now = LocalDateTime.now();

        MessageEntity message = new MessageEntity();
        message.setConversationId(conversation.getId());
        message.setRole(request.getRole().trim());
        message.setContent(request.getContent());
        message.setMetadata(request.getMetadata());
        message.setCreatedAt(now);
        messageMapper.insert(message);

        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);
        return MessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(String chatId) {
        ConversationEntity conversation = requireByChatId(chatId);
        return messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversation.getId())
                        .orderByAsc(MessageEntity::getCreatedAt))
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    private ConversationEntity requireByChatId(String chatId) {
        ConversationEntity entity = conversationMapper.selectOne(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getChatId, chatId)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new IllegalArgumentException("会话不存在: " + chatId);
        }
        return entity;
    }
}
