# 3.2 缓存 - Redis 学习与实践

> 对应学习路线：阶段 3.2

---

## 📚 学习目标

1. 理解为什么缓存应该优先加在“高频读、低频写”的链路上
2. 掌握 Spring Cache + Redis 的最小落地方式
3. 学会按业务拆分缓存名、TTL 和失效策略
4. 理解缓存与鉴权链路的结合方式
5. 能把当前项目的缓存实现、配置和验证方式说清楚

---

## 🎯 本次实践做了什么

当前项目这次没有去缓存复杂分页结果，而是优先给两个更稳定的热点读取点加缓存：

| 缓存名 | 位置 | 作用 | TTL |
|--------|------|------|-----|
| `userById` | `UserServiceImpl#getById` | 缓存用户详情 | 30 分钟 |
| `userByUsername` | `UserServiceImpl#findByUsername` | 缓存按用户名查用户 | 10 分钟 |

对应改动点：

| 文件 | 说明 |
|------|------|
| `pom.xml` | 新增 `spring-boot-starter-cache`、`spring-boot-starter-data-redis` |
| `SpringbootDemoApplication.java` | 开启 `@EnableCaching` |
| `CacheConfig.java` | 统一 Redis Cache 序列化和 TTL 配置 |
| `UserServiceImpl.java` | 查询加 `@Cacheable`，写操作加 `@CacheEvict` / `@Caching` |
| `JwtAuthenticationFilter.java` | 每次请求按用户名取当前用户，缓存直接参与鉴权链路 |
| `application-dev.yml` / `application-prod.yml` | 启用 Redis 缓存 |
| `application-test.yml` | 测试环境降级为 `simple` 缓存，避免依赖外部 Redis |

---

## 🤔 为什么选这两个缓存点

### 1. `getById`

这是最标准的缓存对象：

- Key 简单
- 命中率高
- 脏数据范围容易控制
- 写操作后可以精确按 ID 清理

### 2. `findByUsername`

这个点一开始看起来只是登录接口会用到，但在当前项目里我们顺手把 JWT 过滤器也改成了“从当前用户表读取角色”：

```text
请求携带 JWT
  -> JWT 解析出 username / userId
  -> UserService.findByUsername(username)
  -> 先查 Redis
  -> 命中则直接恢复当前用户角色
  -> 未命中才查数据库
```

这样做有两个好处：

1. 权限判断基于当前数据库中的用户角色，而不是只信任旧 Token 里的角色
2. 每次请求虽然都要查用户，但大部分命中 Redis，不会持续压数据库

---

## 🧩 实现细节

### 1. 引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. 开启缓存

```java
@SpringBootApplication
@EnableCaching
public class SpringbootDemoApplication {
}
```

### 3. Redis Cache 统一配置

当前项目新增了 `demo.config.CacheConfig`：

```java
@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    public static final String USER_BY_ID_CACHE = "userById";
    public static final String USER_BY_USERNAME_CACHE = "userByUsername";

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(...)
                .serializeValuesWith(...)
                .entryTtl(Duration.ofMinutes(15));
    }
}
```

设计点：

- Key 使用字符串序列化
- Value 使用 JSON 序列化，便于排查
- 不缓存 `null`
- 默认 TTL 15 分钟，再对不同缓存名单独覆盖

### 4. 按缓存名设置 TTL

```java
builder
    .withCacheConfiguration("userById", redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
    .withCacheConfiguration("userByUsername", redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)));
```

为什么 `userByUsername` 更短？

- 它直接影响登录态和角色恢复
- 用户改角色后，希望尽快生效
- 10 分钟是当前学习项目里比较稳妥的折中值

---

## 🔍 Service 层缓存实现

### 1. 查询加缓存

```java
@Override
@Cacheable(cacheNames = CacheConfig.USER_BY_ID_CACHE, key = "#id", unless = "#result == null")
public User getById(Serializable id) {
    return super.getById(id);
}
```

```java
@Override
@Cacheable(cacheNames = CacheConfig.USER_BY_USERNAME_CACHE, key = "#username", unless = "#result == null")
public User findByUsername(String username) {
    ...
}
```

### 2. 写操作清缓存

当前项目对这些写方法做了失效处理：

| 方法 | 处理策略 |
|------|----------|
| `save` | 清理 `userById` 对应 ID 和 `userByUsername` |
| `updateById` | 清理 `userById` 对应 ID 和 `userByUsername` |
| `removeById` | 清理 `userById` 对应 ID 和 `userByUsername` |
| `register` | 注册后清理相关缓存 |
| `updateAvatar` | 头像更新后清理相关缓存 |

这里对 `userByUsername` 采用了 `allEntries = true` 的保守策略。原因很简单：更新用户时，用户名可能被改掉，单靠新参数并不总能准确定位旧缓存键。

对于当前学习项目，用户写操作频率低，这个取舍是合理的；如果后续进入更高并发场景，再优化成“精确删除旧用户名缓存”即可。

---

## 🔐 缓存与 JWT 鉴权的结合

之前过滤器只要 Token 合法，就直接使用 Token 里的角色；现在改成：

```text
Token 合法
  -> 解析 username / userId
  -> 根据 username 查询当前用户
  -> 用户不存在或 userId 不匹配：拒绝认证
  -> 取数据库/缓存中的角色，构造权限集合
```

这让缓存不只是“给某个查询加速”，而是直接参与到安全链路：

- 用户被删除后，旧 Token 不会继续长期生效
- 用户角色变更后，权限刷新更可控
- Redis 命中时，鉴权请求不会每次都落库

---

## ⚙️ 配置说明

### 开发 / 生产环境

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      timeout: 3s
  cache:
    type: redis
    redis:
      cache-null-values: false
      key-prefix: "springboot-demo:"
```

说明：

- 默认连本地 `127.0.0.1:6379`
- 也可以通过环境变量 `REDIS_HOST`、`REDIS_PORT` 覆盖
- 所有缓存 Key 带统一前缀，便于区分业务

### 测试环境

```yaml
spring:
  cache:
    type: simple
```

这样测试环境不需要依赖真实 Redis，也不会因为本地没启动 Redis 导致测试启动失败。

---

## 🧪 本地验证建议

### 1. 先启动 Redis

如果你本机已安装 Redis，确保 `6379` 可访问即可。

### 2. 启动项目后做两次相同请求

推荐观察两条链路：

1. 连续调用登录接口，观察第二次 `findByUsername` 是否不再打印 SQL
2. 连续执行同一个 `getById` 场景，观察第二次是否直接命中缓存

### 3. 再做一次写操作

例如更新用户、删除用户或修改头像，再次读取，看是否触发重新查库。

核心观察点：

- 第一次查库，第二次走缓存
- 写操作后缓存失效
- Redis 中能看到 `springboot-demo:userById::...` 这类 Key

---

## 🛡️ 这次实践里刻意没做的事

### 1. 不缓存分页结果

像 `/user/page` 这种接口参数很多：

- `pageNo`
- `pageSize`
- `name`
- `age`
- `phone`
- `start`
- `end`

如果直接缓存，Key 会迅速膨胀，而且命中率未必高。当前项目更适合先把“用户详情”和“鉴权用户查询”做扎实。

### 2. 不缓存空值

当前实现用了 `disableCachingNullValues()`，因为这个项目现阶段更关注简单和可控。

如果后面遇到明显的缓存穿透场景，再考虑：

1. 短 TTL 空值缓存
2. 布隆过滤器

---

## ⚠️ 常见问题

### 1. 项目启动正常，但第一次访问缓存接口就报 Redis 连接错误

原因：开发环境已经启用了 `spring.cache.type=redis`，但本地 Redis 没启动。

### 2. 更新用户后，旧角色短时间仍然生效

如果请求落在 `userByUsername` 的 TTL 窗口内，最多会有短时间缓存延迟。当前项目通过“写操作清缓存 + 较短 TTL”来降低这个问题。

### 3. 为什么不直接缓存 Controller 返回值

缓存应该尽量放在 Service 层，避免和 HTTP 结构、统一响应包装、参数绑定强耦合。

---

## 📝 本次实践结论

这次 Redis 实践最重要的不是“把注解贴上去”，而是做了两个明确取舍：

1. **只缓存真正高频、键简单的读取点**
2. **让缓存服务于当前鉴权链路，而不是只做一个演示用例**

当前项目已经具备一个比较实用的最小缓存闭环：

```text
查询 -> Redis 命中 / 回源数据库 -> 写操作清缓存 -> 后续再次回填
```

下一步如果继续深入，最适合补的是：

1. 验证码缓存（`StringRedisTemplate`）
2. Token 黑名单
3. 分布式锁或热点 Key 保护

---

## 📚 参考资料

- [Spring Cache Reference](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
- [Redis 官方文档](https://redis.io/docs/)
