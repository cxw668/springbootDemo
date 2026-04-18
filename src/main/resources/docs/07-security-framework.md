# 3.1 安全框架 - 学习与实践

> 对应学习路线：阶段 3.1

---

## 📚 学习目标

1. 理解认证（Authentication）与授权（Authorization）的区别
2. 熟悉 Spring Security 基本组件与过滤器链
3. 掌握 JWT 登录态设计与无状态鉴权流程
4. 能基于角色和权限实现 RBAC 访问控制
5. 能为当前用户管理项目补齐登录、登出、权限校验能力

---

## 🎯 核心知识点

### 1. 安全模型

| 概念 | 说明 | 示例 |
|------|------|------|
| 认证 | 你是谁 | 用户名密码登录 |
| 授权 | 你能做什么 | 只有管理员可删除用户 |
| 凭证 | 用来证明身份 | JWT、Session、Token |
| 主体 | 当前登录用户 | `Authentication#getPrincipal()` |

### 2. Spring Security 关键组件

| 组件 | 作用 |
|------|------|
| `SecurityFilterChain` | 定义请求安全规则 |
| `AuthenticationManager` | 执行认证逻辑 |
| `UserDetailsService` | 加载用户信息 |
| `PasswordEncoder` | 处理密码加密与匹配 |
| `OncePerRequestFilter` | 自定义 JWT 过滤器 |

### 3. JWT 鉴权流程

```text
登录请求
  -> AuthenticationManager 校验用户名/密码
  -> 认证成功后签发 JWT
  -> 前端保存 Token
  -> 后续请求在 Authorization: Bearer xxx 中携带 JWT
  -> JWT 过滤器解析 Token
  -> 将用户身份写入 SecurityContext
  -> 控制器/方法级权限判断
```

### 4. RBAC 模型

推荐最小模型：

```text
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
```

常见映射：

| 角色 | 权限示例 |
|------|----------|
| `ROLE_ADMIN` | `user:create`、`user:update`、`user:delete` |
| `ROLE_OPERATOR` | `user:update`、`user:view` |
| `ROLE_VIEWER` | `user:view` |

---

## 🔧 当前项目落地建议

### 推荐路线

对于当前 Spring Boot 单体项目，优先选择：

1. **Spring Security + JWT**：贴近主流企业实现，适合理解过滤器链与权限模型
2. **先做粗粒度角色控制，再做细粒度权限控制**
3. **先保护用户管理接口，再扩展到文件上传等高风险接口**

### 推荐接口范围

| 接口 | 是否鉴权 | 说明 |
|------|----------|------|
| `/auth/login` | 否 | 登录入口 |
| `/auth/logout` | 是 | 前端登出或服务端加入黑名单 |
| `/user/page` | 是 | 需要查看用户权限 |
| `/user/{id}` 删除 | 是 | 需要管理员权限 |
| `/api/files/**` | 是 | 防止任意文件上传 |
| `/swagger-ui/**` | 可选 | 开发环境开放，生产环境关闭或鉴权 |

---

## 🧩 实践步骤

### 步骤 1：引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

### 步骤 2：设计认证数据

建议补充字段：

```sql
ALTER TABLE user
ADD COLUMN password VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
ADD COLUMN enabled TINYINT DEFAULT 1 COMMENT '是否启用';
```

不要明文存储密码，统一使用 `BCryptPasswordEncoder`。

### 步骤 3：配置 SecurityFilterChain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(config ->
                    config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/user/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

### 步骤 4：实现登录与发 Token

登录成功后返回：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

### 步骤 5：实现方法级权限控制

```java
@PreAuthorize("hasAuthority('user:delete')")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) {
    ...
}
```

### 步骤 6：统一异常返回

需要分别处理：

| 场景 | 建议返回 |
|------|----------|
| 未登录 | 401 |
| Token 失效 | 401 |
| 权限不足 | 403 |
| 账号禁用 | 403 |

保持与项目现有 `Result<T>` 响应结构一致。

---

## 🧪 建议练习

1. 实现 `/auth/login`，返回 JWT
2. 给 `/user/page` 增加登录校验
3. 给删除接口增加 `ADMIN` 角色限制
4. 给文件上传接口增加 `user:upload` 权限
5. 增加一个测试用户和一个管理员用户，验证权限差异

---

## ⚠️ 常见问题

### 1. 密码加密后无法登录

原因通常是注册时未加密、登录时却使用了 `BCryptPasswordEncoder` 比对。

### 2. JWT 解析成功但拿不到权限

说明只恢复了用户名，没有把角色/权限集合写回 `Authentication`。

### 3. 过滤器执行了但接口仍返回 403

需要检查：

1. `SecurityContextHolder` 是否成功写入认证对象
2. 权限名是否和 `@PreAuthorize` 表达式一致
3. 角色是否带 `ROLE_` 前缀

---

## 📝 学习总结

- Spring Security 的核心不是“配置很多”，而是理解过滤器链
- JWT 解决的是“无状态登录态”问题，不会自动解决权限设计问题
- 企业项目里常见模式是：**JWT 做身份认证，RBAC 做权限控制**
- 当前项目最适合先做登录 + 用户接口鉴权，再逐步扩展到文件上传与后台管理接口

---

## 📚 参考资料

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JWT 官方说明](https://jwt.io/introduction)

