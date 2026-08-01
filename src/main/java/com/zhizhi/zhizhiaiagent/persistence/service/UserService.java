package com.zhizhi.zhizhiaiagent.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.persistence.dto.CreateUserRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.UserEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.UserMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private  UserMapper userMapper;

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

        UserEntity entity = new UserEntity();
        entity.setId(IdGenerator.nextId());
        entity.setUsername(username);
        // D4 再替换为真正哈希；此处仅占位落库
        entity.setPasswordHash(StringUtils.hasText(request.getPassword()) ? request.getPassword() : null);
        entity.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : username);
        entity.setStatus(1);
        AuditHelper.fillOnCreate(entity, request.getCreateBy(), request.getEnterpriseId());
        userMapper.insert(entity);
        return UserResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(String id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return UserResponse.from(entity);
    }
}
