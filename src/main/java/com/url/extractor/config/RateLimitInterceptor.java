package com.url.extractor.config;

import com.url.extractor.utils.MyLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Long> requestCounts = new ConcurrentHashMap<>();
    private final long REQUEST_LIMIT = 1; // Max requests allowed in a minute

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {

        String ipAddress = request.getRemoteAddr();
        long currentTime = System.currentTimeMillis() / 1000;

        MyLogger.info("REQUESTED BY : " + ipAddress);

        String key = ipAddress + ":" + currentTime;

        if (requestCounts.containsKey(key)) {
            long count = requestCounts.get(key);
            if (count >= REQUEST_LIMIT) {
                MyLogger.warn("ERROR : " + HttpStatus.TOO_MANY_REQUESTS);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("TOO MANY REQUESTS");
                return false;
            } else {
                requestCounts.put(key, count + 1);
            }
        } else {
            requestCounts.put(key, 1L);
        }
        return true;
    }
}

