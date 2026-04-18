package demo.controller;

import demo.common.BizCode;
import demo.common.BizException;
import demo.common.Result;
import demo.config.JwtProperties;
import demo.model.User;
import demo.security.JwtUtil;
import demo.service.IUserService;
import jakarta.validation.Valid;
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
    private final IUserService userService;
    private final JwtProperties jwtProperties;

    public AuthController(JwtUtil jwtUtil, PasswordEncoder passwordEncoder, 
                         IUserService userService, JwtProperties jwtProperties) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求（包含用户名和密码）
     * @return JWT Token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());
        
        // 1. 根据用户名查询用户
        User user = userService.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new BizException(BizCode.INVALID_CREDENTIALS);
        }
        
        // 2. 验证密码是否正确
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BizException(BizCode.INVALID_CREDENTIALS);
        }
        
        // 3. 获取用户角色
        String role = user.getRole() != null ? user.getRole() : "USER";
        
        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), role);
        
        // 5. 构建响应
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", jwtProperties.getExpiration());
        data.put("username", user.getUsername());
        data.put("userId", user.getId());
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
    public Result<Map<String, Object>> register(@RequestBody @Valid RegisterRequest registerRequest) {
        log.info("用户注册请求: {}", registerRequest.getUsername());
        
        // 1. 验证用户名是否已存在（在 Service 层处理）
        // 2. 构建用户对象
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setRole(registerRequest.getRole());
        
        // 3. 注册用户（Service 层会加密密码并保存）
        User registeredUser = userService.register(user);
        
        // 4. 构建响应（不包含敏感信息）
        Map<String, Object> data = new HashMap<>();
        data.put("userId", registeredUser.getId());
        data.put("username", registeredUser.getUsername());
        data.put("name", registeredUser.getName());
        data.put("email", registeredUser.getEmail());
        data.put("role", registeredUser.getRole());
        data.put("createTime", registeredUser.getCreateTime());
        
        return Result.success("注册成功", data);
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
    public static class LoginRequest {
        @jakarta.validation.constraints.NotBlank(message = "用户名不能为空")
        private String username;
        
        @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 注册请求 DTO
     */
    @Data
    public static class RegisterRequest {
        @jakarta.validation.constraints.NotBlank(message = "用户名不能为空")
        @jakarta.validation.constraints.Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
        private String username;
        
        @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
        @jakarta.validation.constraints.Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
        private String password;
        
        @jakarta.validation.constraints.NotBlank(message = "姓名不能为空")
        @jakarta.validation.constraints.Size(min = 1, max = 50, message = "姓名长度必须在1-50之间")
        private String name;
        
        private String email;
        
        private String role;
    }
}
