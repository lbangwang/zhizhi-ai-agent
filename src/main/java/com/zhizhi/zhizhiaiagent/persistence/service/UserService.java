package com.zhizhi.zhizhiaiagent.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateUserRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.UserEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("username 不能为空");
        }
        String username = request.getUsername().trim();
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("username 已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        // D4 再替换为真正哈希；此处仅占位落库
        entity.setPasswordHash(StringUtils.hasText(request.getPassword()) ? request.getPassword() : null);
        entity.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : username);
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);
        return UserResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return UserResponse.from(entity);
    }
}
