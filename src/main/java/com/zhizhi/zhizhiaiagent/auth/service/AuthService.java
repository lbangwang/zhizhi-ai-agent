package com.zhizhi.zhizhiaiagent.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhizhi.zhizhiaiagent.auth.dto.LoginRequest;
import com.zhizhi.zhizhiaiagent.auth.dto.LoginResponse;
import com.zhizhi.zhizhiaiagent.auth.dto.RegisterRequest;
import com.zhizhi.zhizhiaiagent.persistence.dto.UserResponse;
import com.zhizhi.zhizhiaiagent.persistence.entity.UserEntity;
import com.zhizhi.zhizhiaiagent.persistence.mapper.UserMapper;
import com.zhizhi.zhizhiaiagent.persistence.support.AuditHelper;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class AuthService {

    @Autowired
    private  UserMapper userMapper;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        //校验用户名和密码
        String username = requireUsername(request.getUsername());
        String password = requirePassword(request.getPassword());

        //数据库是否存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        //设置用户信息
        UserEntity entity = new UserEntity();
        entity.setId(IdGenerator.nextId());
        entity.setUsername(username);
        entity.setPasswordHash(BCrypt.hashpw(password));
        entity.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : username);
        entity.setStatus(1);
        AuditHelper.fillOnCreate(entity, username, null);
        userMapper.insert(entity);

        //返回用户和token相关信息
        return loginByUser(entity);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = requireUsername(request.getUsername());
        String password = requirePassword(request.getPassword());

        UserEntity entity = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1"));
        if (entity == null || !StringUtils.hasText(entity.getPasswordHash())
                || !BCrypt.checkpw(password, entity.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            throw new IllegalArgumentException("账号已禁用");
        }
        return loginByUser(entity);
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        String userId = StpUtil.getLoginIdAsString();
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return UserResponse.from(entity);
    }

    private LoginResponse loginByUser(UserEntity entity) {
        StpUtil.login(entity.getId());
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .tokenName(StpUtil.getTokenName())
                .tokenPrefix("Bearer")
                .user(UserResponse.from(entity))
                .build();
    }

    private static String requireUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String value = username.trim();
        if (value.length() < 3 || value.length() > 32) {
            throw new IllegalArgumentException("用户名长度需为 3~32");
        }
        if (!value.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("用户名仅支持字母、数字、下划线");
        }
        return value;
    }

    private static String requirePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 64) {
            throw new IllegalArgumentException("密码长度需为 6~64");
        }
        return password;
    }
}
