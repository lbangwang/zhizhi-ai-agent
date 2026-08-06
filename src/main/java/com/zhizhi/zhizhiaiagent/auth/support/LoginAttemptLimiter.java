package com.zhizhi.zhizhiaiagent.auth.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败轻量限流（进程内计数）。
 * 同 IP / 用户名在窗口内失败次数超限则暂时拒绝登录。
 */
@Component
public class LoginAttemptLimiter {

    private final int maxFailures;
    private final long lockMillis;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(
            @Value("${app.auth.login-max-failures:5}") int maxFailures,
            @Value("${app.auth.login-lock-seconds:900}") long lockSeconds) {
        this.maxFailures = Math.max(1, maxFailures);
        this.lockMillis = Math.max(60, lockSeconds) * 1000L;
    }

    public void assertAllowed(String clientIp, String username) {
        maybeCleanup();
        long now = System.currentTimeMillis();
        checkKey("ip:" + normalizeIp(clientIp), now);
        checkKey("user:" + normalizeUsername(username), now);
    }

    public void recordFailure(String clientIp, String username) {
        long now = System.currentTimeMillis();
        bump("ip:" + normalizeIp(clientIp), now);
        bump("user:" + normalizeUsername(username), now);
    }

    public void clear(String clientIp, String username) {
        attempts.remove("ip:" + normalizeIp(clientIp));
        attempts.remove("user:" + normalizeUsername(username));
    }

    private void checkKey(String key, long now) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        if (now - attempt.windowStartMillis > lockMillis) {
            attempts.remove(key, attempt);
            return;
        }
        if (attempt.failures >= maxFailures) {
            long remainSec = Math.max(1, (lockMillis - (now - attempt.windowStartMillis) + 999) / 1000);
            throw new IllegalArgumentException("登录尝试过于频繁，请 " + remainSec + " 秒后再试");
        }
    }

    private void bump(String key, long now) {
        attempts.compute(key, (k, prev) -> {
            if (prev == null || now - prev.windowStartMillis > lockMillis) {
                return new Attempt(1, now);
            }
            return new Attempt(prev.failures + 1, prev.windowStartMillis);
        });
    }

    private void maybeCleanup() {
        if (attempts.size() < 2000) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Attempt> e = it.next();
            if (now - e.getValue().windowStartMillis > lockMillis) {
                it.remove();
            }
        }
    }

    private static String normalizeIp(String ip) {
        return StringUtils.hasText(ip) ? ip.trim() : "unknown";
    }

    private static String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim().toLowerCase() : "unknown";
    }

    private static final class Attempt {
        private final int failures;
        private final long windowStartMillis;

        private Attempt(int failures, long windowStartMillis) {
            this.failures = failures;
            this.windowStartMillis = windowStartMillis;
        }
    }
}
