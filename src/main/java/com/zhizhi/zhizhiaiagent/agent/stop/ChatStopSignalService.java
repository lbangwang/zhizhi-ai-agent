package com.zhizhi.zhizhiaiagent.agent.stop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话停止信号。
 * <ul>
 *   <li>{@code REDIS_ENABLED=true}：写入 Redis，多实例可共享</li>
 *   <li>未启用 Redis：进程内 ConcurrentHashMap 兜底，单机仍可取消 Agent step</li>
 * </ul>
 */
@Slf4j
@Service
public class ChatStopSignalService {

    //单机服务可以使用map共享状态，但是在跨服务之间就只能用redis
    private final Map<String, Boolean> localStops = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final String keyPrefix;
    private final Duration ttl;

    public ChatStopSignalService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${app.redis.enabled:false}") boolean redisEnabled,
            @Value("${app.redis.stop-key-prefix:zhizhi:chat:stop:}") String keyPrefix,
            @Value("${app.redis.stop-ttl-seconds:600}") long stopTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled && redisTemplate != null;
        this.keyPrefix = keyPrefix;
        this.ttl = Duration.ofSeconds(Math.max(30, stopTtlSeconds));
        log.info("ChatStopSignalService ready, redisEnabled={}", this.redisEnabled);
    }

    /** 请求停止：写入停止标记（下一步循环将退出） */
    public void requestStop(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            log.warn("requestStop ignored: empty chatId");
            return;
        }
        String id = chatId.trim();
        localStops.put(id, Boolean.TRUE);
        if (redisEnabled) {
            try {
                redisTemplate.opsForValue().set(key(id), "1", ttl);
            } catch (Exception e) {
                log.warn("Redis requestStop failed, fallback to local only, chatId={}", id, e);
            }
        }
        log.info("Stop signal set for chatId={}, redis={}", id, redisEnabled);
    }

    /** 是否已请求停止 */
    public boolean shouldStop(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            return false;
        }
        String id = chatId.trim();
        if (Boolean.TRUE.equals(localStops.get(id))) {
            return true;
        }
        if (!redisEnabled) {
            return false;
        }
        try {
            return "1".equals(redisTemplate.opsForValue().get(key(id)));
        } catch (Exception e) {
            log.warn("Redis shouldStop failed, chatId={}", id, e);
            return false;
        }
    }

    /** 开始新一轮对话前清除停止标记 */
    public void clear(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            return;
        }
        String id = chatId.trim();
        localStops.remove(id);
        if (redisEnabled) {
            try {
                redisTemplate.delete(key(id));
            } catch (Exception e) {
                log.warn("Redis clear stop failed, chatId={}", id, e);
            }
        }
    }

    private String key(String chatId) {
        return keyPrefix + chatId;
    }
}
