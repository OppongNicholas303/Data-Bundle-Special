package com.space.space_bundle.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-process rate limiter for sensitive auth endpoints.
 * Limits each IP to 10 attempts per minute on /auth/login and /auth/register.
 * For production with multiple instances, replace with Redis-backed rate limiting.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!isRateLimitedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        String key = ip + ":" + path;

        RequestCounter counter = counters.computeIfAbsent(key, k -> new RequestCounter());

        if (counter.isExceeded()) {
            log.warn("[RATE_LIMIT] Blocked IP={} path={}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        counter.increment();
        chain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return path.contains("/auth/login")
                || path.contains("/auth/register")
                || path.contains("/auth/forgot-password")
                || path.contains("/auth/reset-password");
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().toEpochMilli();

        void increment() {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart > WINDOW_MS) {
                count.set(0);
                windowStart = now;
            }
            count.incrementAndGet();
        }

        boolean isExceeded() {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart > WINDOW_MS) return false;
            return count.get() >= MAX_REQUESTS;
        }
    }
}
