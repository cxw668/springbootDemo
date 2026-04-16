# 核心进阶功能实现

> 对应学习路线：阶段 2.3、2.4、2.5

---

## 2.3 拦截器/过滤器

### 实现概述

- 创建 `WebMvcConfig` 统一注册拦截器
- 补全全局 CORS 跨域配置
- 路径白名单 + IP 白名单双模式鉴权

### 架构图

```
请求进入
  │
  ▼
┌─────────────────────────────────────────┐
│          WebMvcConfig                   │
│  ┌───────────────────────────────────┐  │
│  │  RequestLoggingInterceptor        │  │
│  │  • 生成 requestId → MDC           │  │
│  │  • 记录请求方法/URI/耗时           │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │  SimpleAuthInterceptor            │  │
│  │  ├─ 路径在白名单？──→ 是 → 放行    │  │
│  │  ├─ IP在白名单？──→ 是 → 放行      │  │
│  │  └─ 检查 Authorization 请求头     │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
  │
  ▼
Controller 处理业务
```

### 文件清单

| 文件 | 说明 |
|------|------|
| `WebMvcConfig.java` | MVC 配置类，注册拦截器 + CORS |
| `RequestLoggingInterceptor.java` | 请求日志拦截器（MDC 链路追踪） |
| `SimpleAuthInterceptor.java` | 鉴权拦截器（路径/IP 白名单 + Token 校验） |
| `AuthProperties.java` | 鉴权配置属性（从 yml 读取） |

### 关键设计

#### CORS 配置

```java
registry.addMapping("/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
```

#### IP 获取策略

```
X-Forwarded-For → X-Real-IP → request.getRemoteAddr()
```

兼容 Nginx 等反向代理场景，多级代理取第一个 IP。

#### IP 白名单配置（application.yaml）

```yaml
security:
  auth:
    token: demo-token-12345
    # 取消注释并添加 IP 即可启用白名单
    # ip-whitelist:
    #   - 127.0.0.1
    #   - 192.168.1.100
```

---

## 2.4 日志管理

### 实现概述

- `logback-spring.xml` 统一日志配置
- 控制台彩色输出 + 文件滚动归档 + 错误单独存储
- MDC requestId 链路追踪
- 按 profile 区分日志级别和输出目标

### 架构图

```
                    ┌─────────────────┐
                    │   SLF4J 门面     │
                    │  (Logger API)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │    Logback      │
                    │   (实现层)       │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
     ┌──────▼──────┐  ┌─────▼──────┐  ┌──────▼──────┐
     │  CONSOLE    │  │    FILE    │  │ ERROR_FILE  │
     │  彩色输出    │  │ 通用日志    │  │ 错误日志    │
     │  开发调试    │  │ 滚动归档    │  │ 告警监控    │
     └─────────────┘  └─────────────┘  └─────────────┘
```

### 文件清单

| 文件 | 说明 |
|------|------|
| `logback-spring.xml` | Logback 主配置（支持 Spring Profile） |
| `application.yaml` | 默认配置（spring.application.name + 日志基础配置） |
| `application-dev.yml` | 开发环境：DEBUG 级别，SQL 输出 |
| `application-prod.yml` | 生产环境：INFO 级别，文件归档 + 错误日志 |
| `application-test.yml` | 测试环境：H2 内存数据库，WARN 级别 |

### 日志输出格式

**控制台（开发环境）：**

```
2026-04-15 15:50:23.456 INFO  [springboot-demo] [abc123def456789] demo.controller.UserController : Request started: GET /user/page from 127.0.0.1
```

**日志文件（生产环境）：**

```
2026-04-15 15:50:23.456 [http-nio-8080-exec-1]  INFO [springboot-demo] [abc123def456789] demo.controller.UserController : Request completed: GET /user/page status=200 duration=333ms
```

### Profile 差异

| Profile | 日志级别 | 输出目标 | 特点 |
|---------|---------|---------|------|
| dev | DEBUG | 控制台 + 文件 | 输出 SQL 语句，方便调试 |
| test | WARN | 仅控制台 | 减少测试干扰 |
| prod | INFO | 控制台 + 文件 + 错误文件 | 错误单独归档，便于告警 |
| default | INFO | 控制台 + 文件 | 未指定 profile 时生效 |

### MDC 链路追踪流程

```
请求进入 → 拦截器生成 requestId
  → MDC.put("requestId", xxx)
  → 响应头写入 X-Request-Id
  → 后续所有日志自动携带 [requestId]
  → 请求结束 → MDC.remove("requestId")  ← 必须清理，防止内存泄漏
```

**排查问题：**

```bash
# 前端拿到 X-Request-Id = abc123def456789
# 搜索完整调用链
grep "abc123def456789" logs/springboot-demo.log
# 只看错误
grep "abc123def456789" logs/springboot-demo-error.log
```

### 日志滚动策略

```xml
<maxFileSize>100MB</maxFileSize>    <!-- 单个文件最大 100MB -->
<maxHistory>30</maxHistory>          <!-- 保留 30 天 -->
<totalSizeCap>3GB</totalSizeCap>     <!-- 总大小上限 3GB，超了自动清理最旧的 -->
```

---

## 2.5 单元测试进阶

### 实现概述

- `UserServiceImplTest` 使用 Mockito 模拟 Mapper 层
- 独立测试 Service 层逻辑，不依赖真实数据库
- 覆盖分页查询 6 种场景 + 继承 CRUD 方法

### 架构图

```
┌──────────────────────────────────────────────┐
│           UserServiceImplTest                │
│                                              │
│  @Mock          @InjectMocks                 │
│  ┌──────┐       ┌──────────────┐             │
│  │User  │◄──────│ UserService  │             │
│  │Mapper│  mock │   Impl       │             │
│  └──────┘       └──────────────┘             │
│                                              │
│  when(mapper.selectPage(...))                │
│       .thenReturn(mockPage)                  │
│                                              │
│  IPage<User> result = service.pageQuery(...) │
│                                              │
│  assertThat(result).hasSize(...)             │
│  verify(mapper).selectPage(...)              │
└──────────────────────────────────────────────┘
```

### 文件清单

| 文件 | 说明 |
|------|------|
| `UserServiceImplTest.java` | Service 层 Mockito 单元测试 |
| `UserControllerTests.java` | Controller 层 MockMvc 集成测试（已有） |

### 测试覆盖

#### 分页查询（6 个用例）

| 测试用例 | 场景 |
|---------|------|
| `testPageQueryNoConditions` | 无条件查询，返回所有数据 |
| `testPageQueryByName` | 按名称模糊查询 |
| `testPageQueryByAge` | 按年龄精确查询 |
| `testPageQueryByPhone` | 按手机号模糊查询 |
| `testPageQueryMultipleConditions` | 多条件组合查询 |
| `testPageQueryEmptyResult` | 空结果分页 |

#### 继承方法（5 个用例）

| 测试用例 | 场景 |
|---------|------|
| `testGetById` | 根据 ID 查询用户 |
| `testGetByIdNotFound` | 查询不存在的用户 |
| `testSave` | 保存用户 |
| `testRemoveById` | 根据 ID 删除用户 |
| `testUpdateById` | 更新用户信息 |

### Mockito 核心注解

```java
@ExtendWith(MockitoExtension.class)  // 启用 Mockito
class UserServiceImplTest {

    @Mock        // 创建 UserMapper 的模拟对象
    private UserMapper userMapper;

    @InjectMocks // 将 @Mock 注入到 UserServiceImpl
    private UserServiceImpl userService;
}
```

### 测试模式

```java
// 1. Arrange：准备模拟数据
when(userMapper.selectPage(any(), any())).thenReturn(mockPage);

// 2. Act：调用待测方法
IPage<User> result = userService.pageQuery(1, 10, null, null, null, null, null);

// 3. Assert：验证结果和行为
assertThat(result.getRecords()).hasSize(2);
verify(userMapper).selectPage(any(), any());
```
