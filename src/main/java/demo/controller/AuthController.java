package demo.controller;

import demo.common.Result;
import demo.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 
 * 提供登录、注册等认证相关接口
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求（包含用户名和密码）
     * @return JWT Token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        // TODO: 这里应该从数据库查询用户信息
        // 示例代码，实际使用时需要替换为真实的用户验证逻辑
        
        log.info("用户登录请求: {}", loginRequest.getUsername());
        
        // 模拟用户验证（实际应该从数据库查询）
        // 1. 根据用户名查询用户
        // 2. 验证密码是否正确
        // 3. 获取用户角色
        
        // 示例：假设验证通过
        String username = loginRequest.getUsername();
        Long userId = 1L;
        String role = "USER"; // 或 "ADMIN"
        
        // 生成 JWT Token
        String token = jwtUtil.generateToken(username, userId, role);
        
        // 构建响应
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", 7200); // 2小时
        data.put("username", username);
        data.put("role", role);
        
        return Result.success("登录成功", data);
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest registerRequest) {
        // TODO: 实现用户注册逻辑
        // 1. 验证用户名是否已存在
        // 2. 加密密码
        // 3. 保存用户到数据库
        
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
        log.info("用户注册: {}, 加密后密码: {}", registerRequest.getUsername(), encodedPassword);
        
        return Result.success("注册成功", null);
    }

    /**
     * 登出（前端删除 Token 即可，服务端可以实现 Token 黑名单）
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // TODO: 如果需要实现 Token 黑名单，可以将 Token 加入 Redis
        return Result.success("登出成功", null);
    }

    /**
     * 登录请求 DTO
     */
    @Data
    @AllArgsConstructor
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 注册请求 DTO
     */
    @Data
    @AllArgsConstructor
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
    }
}
