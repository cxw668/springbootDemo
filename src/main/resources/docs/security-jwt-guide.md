# Spring Security + JWT 配置说明

## 📋 已创建的文件

### 1. 核心配置文件
- **SecurityConfig.java** - Spring Security 主配置类
- **JwtProperties.java** - JWT 配置属性类
- **JwtUtil.java** - JWT 工具类（生成、解析、验证 Token）
- **JwtAuthenticationFilter.java** - JWT 认证过滤器

### 2. 示例接口
- **AuthController.java** - 登录/注册/登出接口

### 3. 依赖配置
- **pom.xml** - 已补充完整的 JJWT 依赖（api + impl + jackson）

### 4. 应用配置
- **application.yaml** - 已添加 JWT 配置项

---

## 🔧 SecurityFilterChain 配置详解

### 核心配置流程

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            // 1. 禁用 CSRF（前后端分离项目不需要）
            .csrf(csrf -> csrf.disable())
            
            // 2. 设置无状态会话管理（使用 JWT，不创建 HttpSession）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                    // 允许匿名访问的路径
                    .requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/auth/register").permitAll()
                    
                    // Swagger/OpenAPI 文档
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    
                    // 删除用户需要 ADMIN 角色
                    .requestMatchers(HttpMethod.DELETE, "/user/**").hasRole("ADMIN")
                    
                    // 其他所有请求都需要认证
                    .anyRequest().authenticated())
            
            // 4. 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 5. 构建
            .build();
}
```

### 关键配置说明

| 配置项 | 作用 | 说明 |
|--------|------|------|
| `csrf().disable()` | 禁用 CSRF 保护 | 前后端分离项目使用 JWT，不需要 CSRF |
| `SessionCreationPolicy.STATELESS` | 无状态会话 | 不创建 HttpSession，每次请求都需要携带 Token |
| `permitAll()` | 允许匿名访问 | 登录、注册等公开接口 |
| `hasRole("ADMIN")` | 角色校验 | 需要指定角色才能访问 |
| `authenticated()` | 需要认证 | 其他所有接口都需要登录 |
| `addFilterBefore()` | 添加过滤器 | 在用户名密码认证之前执行 JWT 验证 |

---

## 🚀 使用步骤

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 测试登录接口

**请求：**
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "username": "testuser",
    "role": "USER"
  }
}
```

### 3. 携带 Token 访问受保护接口

**请求：**
```http
GET http://localhost:8080/user/page
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 4. 使用方法级权限控制

在 Controller 或 Service 方法上使用注解：

```java
// 需要 ADMIN 角色
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    // ...
}

// 需要特定权限
@PreAuthorize("hasAuthority('user:create')")
@PostMapping
public Result<Void> createUser(@RequestBody User user) {
    // ...
}
```

---

## 🔐 JWT 工作流程

```
1. 用户登录
   ↓
2. 验证用户名密码
   ↓
3. 生成 JWT Token（包含 userId、username、role）
   ↓
4. 返回 Token 给前端
   ↓
5. 前端保存 Token（localStorage / sessionStorage）
   ↓
6. 后续请求在 Header 中携带 Token
   Authorization: Bearer <token>
   ↓
7. JwtAuthenticationFilter 拦截请求
   ↓
8. 解析并验证 Token
   ↓
9. 将用户信息写入 SecurityContext
   ↓
10. 控制器通过 @PreAuthorize 进行权限校验
```

---

## ⚙️ 配置说明

### application.yaml 中的 JWT 配置

```yaml
jwt:
  secret: mySecretKeyForSpringBootDemo2024VeryLong  # 密钥（至少32字符）
  expiration: 7200000  # Token 过期时间（毫秒），默认2小时
```

### 生产环境建议

1. **密钥管理**：不要硬编码在代码中，使用环境变量
   ```yaml
   jwt:
     secret: ${JWT_SECRET:default-secret-key}
   ```

2. **设置环境变量**：
   ```bash
   export JWT_SECRET="your-complex-secret-key-here"
   ```

3. **Token 过期时间**：根据业务需求调整
   - 短期 Token：30分钟 - 2小时
   - 长期 Token：7天 - 30天（配合刷新 Token 机制）

---

## 🛡️ 安全注意事项

### 1. 密钥安全
- ✅ 使用至少 32 个字符的复杂密钥
- ✅ 生产环境从环境变量或配置中心读取
- ❌ 不要将密钥提交到版本控制系统

### 2. Token 存储
- ✅ 前端使用 HttpOnly Cookie 或 localStorage
- ✅ 设置合理的过期时间
- ❌ 不要在 URL 中传递 Token

### 3. HTTPS
- ✅ 生产环境必须使用 HTTPS
- ❌ 不要在 HTTP 上传输 Token

### 4. 权限控制
- ✅ 后端必须进行权限校验（不能只靠前端隐藏按钮）
- ✅ 使用 `@PreAuthorize` 进行细粒度控制
- ❌ 不要信任前端传来的角色信息

---

## 📝 常见问题

### Q1: 如何获取当前登录用户信息？

```java
@GetMapping("/me")
public Result<Map<String, Object>> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("username", username);
    userInfo.put("authorities", authentication.getAuthorities());
    
    return Result.success(userInfo);
}
```

### Q2: 如何实现 Token 刷新？

方案：
1. 发放两个 Token：Access Token（短期）+ Refresh Token（长期）
2. Access Token 过期后，使用 Refresh Token 换取新的 Access Token
3. Refresh Token 存储在 Redis 中，可主动撤销

### Q3: 如何实现 Token 黑名单（强制登出）？

```java
// 登出时将 Token 加入 Redis 黑名单
redisTemplate.opsForValue().set(
    "blacklist:" + token, 
    "logout", 
    expiration, 
    TimeUnit.MILLISECONDS
);

// 过滤器中检查黑名单
if (redisTemplate.hasKey("blacklist:" + token)) {
    throw new RuntimeException("Token 已失效");
}
```

### Q4: 401 和 403 的区别？

- **401 Unauthorized**：未登录或 Token 无效
- **403 Forbidden**：已登录但权限不足

---

## 🎯 下一步优化方向

1. **实现真实的用户认证**
   - 连接数据库查询用户
   - 验证 BCrypt 加密的密码

2. **实现 Token 刷新机制**
   - Access Token + Refresh Token

3. **实现 Token 黑名单**
   - 使用 Redis 存储已登出的 Token

4. **完善异常处理**
   - 自定义 AuthenticationEntryPoint 处理 401
   - 自定义 AccessDeniedHandler 处理 403

5. **添加审计日志**
   - 记录登录、登出、权限变更等操作

---

## 📚 参考资料

- [Spring Security 官方文档](https://docs.spring.io/spring-security/reference/)
- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [JWT 官网](https://jwt.io/)
