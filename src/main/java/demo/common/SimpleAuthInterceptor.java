package demo.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简单鉴权拦截器（演示用）
 * 功能：
 * 1. 检查 Authorization 请求头
 * 2. 验证 token 格式（Bearer <token>）
 * 3. 路径白名单跳过鉴权（/user/page、/user/search 等公开接口）
 * 4. IP白名单跳过鉴权（trust的IP直接放行，在 application.yml 中配置）
 * 5. 实际项目中应替换为 JWT 验证或 Spring Security
 */
@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SimpleAuthInterceptor.class);

    private final AuthProperties authProperties;

    public SimpleAuthInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        // 白名单：路径或IP在白名单中，跳过鉴权
        if (isPathWhiteListed(uri) || isIpWhiteListed(clientIp)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized access to {}: no Authorization header", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未提供认证令牌\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(7);
        if (!authProperties.getToken().equals(token)) {
            log.warn("Unauthorized access to {}: invalid token", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"认证令牌无效\",\"data\":null}");
            return false;
        }

        log.info("Authorized access to {}", uri);
        return true;
    }

    /**
     * 判断是否为路径白名单
     */
    private boolean isPathWhiteListed(String uri) {
        return uri.startsWith("/user/page")
                || uri.startsWith("/user/search")
                || uri.equals("/actuator/health");
    }

    /**
     * 判断是否为 IP 白名单（从配置文件读取）
     */
    private boolean isIpWhiteListed(String ip) {
        return authProperties.getIpWhitelist().contains(ip);
    }

    /**
     * 获取客户端真实IP
     * 优先从代理头中获取（Nginx等反向代理场景），否则取远程地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时，X-Forwarded-For 可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
