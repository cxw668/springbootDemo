package demo.config;

import demo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 
 * 核心功能：
 * 1. 禁用 CSRF（前后端分离项目不需要）
 * 2. 设置无状态会话（使用 JWT）
 * 3. 配置请求授权规则
 * 4. 添加 JWT 认证过滤器
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级权限控制 @PreAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置 SecurityFilterChain
     * 
     * 关键配置说明：
     * - csrf().disable(): 前后端分离项目禁用 CSRF 保护
     * - sessionManagement().stateless(): 无状态会话，不创建 HttpSession
     * - authorizeHttpRequests(): 定义请求的访问权限
     * - addFilterBefore(): 在用户名密码认证过滤器之前添加 JWT 过滤器
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 1. 禁用 CSRF（JWT 不需要 CSRF 保护）
                .csrf(AbstractHttpConfigurer::disable)
                
                // 2. 设置无状态会话管理
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 3. 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 允许匿名访问的路径
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/register").permitAll()
                        
                        // Swagger/OpenAPI 文档（开发环境开放）
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        
                        // H2 控制台（如果使用 H2 数据库）
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // 健康检查端点
                        .requestMatchers("/actuator/health").permitAll()
                        
                        // 删除用户需要 ADMIN 角色
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/user/**")
                            .hasRole("ADMIN")
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated())
                
                // 4. 添加 JWT 认证过滤器（在 UsernamePasswordAuthenticationFilter 之前执行）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // 5. 构建 SecurityFilterChain
                .build();
    }

    /**
     * 配置密码编码器
     * 
     * 使用 BCrypt 加密算法，这是 Spring Security 推荐的密码加密方式
     * BCrypt 特点：
     * - 自动加盐
     * - 可调节加密强度
     * - 单向加密，不可逆
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
