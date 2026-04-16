package demo.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.UUID;

/**
 * 请求日志拦截器
 * 功能：
 * 1. 为每个请求生成唯一 requestId 并放入 MDC
 * 2. 记录请求方法、URI、耗时、响应状态
 * 3. 将 requestId 写入响应头 X-Request-Id
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 生成唯一请求ID
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        // 写入响应头
        response.setHeader("X-Request-Id", requestId);

        // 记录请求开始时间
        request.setAttribute("startTime", System.currentTimeMillis());

        log.info("Request started: {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        log.info("Request completed: {} {} status={} duration={}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

        // 清理 MDC，防止内存泄漏
        MDC.remove(MDC_REQUEST_ID_KEY);
    }
}
