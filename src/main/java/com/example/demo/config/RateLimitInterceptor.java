package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, RateLimitEntry> requests = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + request.getRequestURI();

        long now = System.currentTimeMillis();
        RateLimitEntry entry = requests.computeIfAbsent(key, k -> new RateLimitEntry(now));

        if (now - entry.windowStart > WINDOW_MS) {
            entry.windowStart = now;
            entry.count.set(0);
        }

        if (entry.count.incrementAndGet() > MAX_REQUESTS) {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\"}");
            response.setContentType("application/json");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        long windowStart;
        AtomicInteger count = new AtomicInteger(0);

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
