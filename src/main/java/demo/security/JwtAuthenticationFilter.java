package demo.security;

import demo.model.User;
import demo.service.IUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 * 
 * 核心功能：
 * 1. 从请求头中提取 JWT Token
 * 2. 验证 Token 的有效性
 * 3. 将用户信息写入 SecurityContext
 * 
 * 执行时机：在 UsernamePasswordAuthenticationFilter 之前执行
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final IUserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, IUserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求头中获取 Token
            String token = extractToken(request);

            // 2. 如果 Token 存在且有效，则设置认证信息
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                // 3. 从 Token 中提取用户信息
                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                User user = userService.findByUsername(username);

                if (user == null || user.getId() == null || !user.getId().equals(userId)) {
                    log.warn("JWT 对应用户不存在或身份不匹配: username={}, userId={}", username, userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                String role = StringUtils.hasText(user.getRole()) ? user.getRole() : "USER";

                // 4. 构建权限列表
                // - 角色权限：ROLE_USER, ROLE_ADMIN
                // - 功能权限：user:upload, user:delete 等
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                
                // 添加角色权限
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                
                // 根据角色添加功能权限
                if ("ADMIN".equals(role)) {
                    // 管理员拥有所有权限
                    authorities.add(new SimpleGrantedAuthority("user:upload"));
                    authorities.add(new SimpleGrantedAuthority("user:delete"));
                    authorities.add(new SimpleGrantedAuthority("user:view"));
                } else if ("USER".equals(role)) {
                    // 普通用户只有上传权限
                    authorities.add(new SimpleGrantedAuthority("user:upload"));
                    authorities.add(new SimpleGrantedAuthority("user:view"));
                }

                // 5. 创建认证对象
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        username,
                        null, // 凭证设为 null，因为已经通过 JWT 验证
                        authorities
                    );

                // 6. 设置请求详情
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 7. 将认证信息存入 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户 {} 认证成功，角色: {}", username, role);
            }
        } catch (Exception e) {
            log.error("JWT 认证失败: {}", e.getMessage());
            // 不抛出异常，让后续过滤器处理未认证的情况
        }

        // 8. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 JWT Token
     * 
     * 支持的格式：
     * - Authorization: Bearer <token>
     * - Authorization: <token>
     *
     * @param request HTTP 请求
     * @return JWT Token，如果不存在则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken)) {
            // 如果以 "Bearer " 开头，去掉前缀
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            // 否则直接返回
            return bearerToken;
        }
        
        return null;
    }
}
