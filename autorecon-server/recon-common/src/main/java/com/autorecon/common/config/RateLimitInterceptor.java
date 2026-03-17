package com.autorecon.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiter using time-window based counters per IP.
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final Map<String, WindowCounter> loginCounters = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> guestCounters = new ConcurrentHashMap<>();

    private static final int LOGIN_MAX_PER_MINUTE = 10;
    private static final int GUEST_MAX_PER_MINUTE = 30;
    private static final long WINDOW_MS = 60_000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (PATH_MATCHER.match("/api/v1/users/login", path)) {
            if (!allow(loginCounters, clientIp, LOGIN_MAX_PER_MINUTE)) {
                response.setStatus(429);
                return false;
            }
        } else if (PATH_MATCHER.match("/api/v1/guest/**", path)) {
            if (!allow(guestCounters, clientIp, GUEST_MAX_PER_MINUTE)) {
                response.setStatus(429);
                return false;
            }
        }
        return true;
    }

    private boolean allow(Map<String, WindowCounter> counters, String key, int maxPerMinute) {
        long now = System.currentTimeMillis();
        counters.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > WINDOW_MS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });
        WindowCounter w = counters.get(key);
        if (w != null && w.count.get() > maxPerMinute) {
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count;

        WindowCounter(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
