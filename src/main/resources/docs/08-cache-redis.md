# 3.2 缓存 - Redis 学习与实践

> 对应学习路线：阶段 3.2

---

## 📚 学习目标

1. 理解缓存的价值与典型使用场景
2. 掌握 Redis 基础数据结构与 Spring Cache 常用注解
3. 理解缓存穿透、击穿、雪崩等常见问题
4. 能为当前项目的用户查询、字典类数据增加缓存
5. 能设计合理的 Key 规范、TTL 和失效策略

---

## 🎯 核心知识点

### 1. 为什么要缓存

```text
客户端请求
  -> 先查 Redis
  -> 命中：直接返回
  -> 未命中：查 MySQL
  -> 回写 Redis
  -> 返回结果
```

缓存适合：

- 读多写少的数据
- 热点数据
- 计算成本高但更新不频繁的数据

### 2. 常用注解

| 注解 | 作用 |
|------|------|
| `@Cacheable` | 查缓存，未命中才执行方法 |
| `@CachePut` | 无论是否命中都执行，并更新缓存 |
| `@CacheEvict` | 删除缓存 |
| `@Caching` | 组合多个缓存操作 |

### 3. Redis 常用数据结构

| 类型 | 场景 |
|------|------|
| String | 用户详情、验证码、Token 黑名单 |
| Hash | 用户资料聚合字段 |
| List | 简单消息队列、操作记录 |
| Set | 去重集合、标签 |
| ZSet | 排行榜、延迟任务 |

---

## 🔧 当前项目落地建议

### 推荐缓存对象

| 数据 | 是否适合缓存 | 建议 |
|------|--------------|------|
| 用户详情 `getById` | 是 | 高频查询，TTL 30 分钟 |
| 用户分页列表 | 谨慎 | 参数多，Key 组合复杂 |
| 用户总数 | 是 | 可短 TTL 缓存 |
| 登录验证码 | 是 | TTL 5 分钟 |
| 用户权限集合 | 是 | 登录后缓存，权限变更时清理 |

### Key 设计规范

推荐格式：

```text
业务名:对象名:关键参数
```

示例：

```text
user:detail:1001
user:permission:1001
user:count:all
```

---

## 🧩 实践步骤

### 步骤 1：引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 步骤 2：配置 Redis

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      timeout: 3s

  cache:
    type: redis
```

### 步骤 3：开启缓存能力

```java
@EnableCaching
@SpringBootApplication
public class SpringbootDemoApplication {
}
```

### 步骤 4：给查询接口加缓存

```java
@Cacheable(value = "userDetail", key = "#id", unless = "#result == null")
public User getUserDetail(Long id) {
    return userMapper.selectById(id);
}
```

### 步骤 5：更新时清理缓存

```java
@CacheEvict(value = "userDetail", key = "#user.id")
public boolean updateUser(User user) {
    return this.updateById(user);
}
```

### 步骤 6：自定义 TTL

建议按缓存类型分组：

| Cache Name | TTL |
|------------|-----|
| `userDetail` | 30 分钟 |
| `userPermission` | 2 小时 |
| `captcha` | 5 分钟 |

---

## 🛡️ 缓存常见风险

### 1. 缓存穿透

**问题**：大量查询不存在的数据，请求每次都打到数据库。

**方案**：

1. 缓存空值，TTL 设短一点
2. 接入布隆过滤器

### 2. 缓存击穿

**问题**：热点 Key 刚好过期，瞬时并发全部打到数据库。

**方案**：

1. 热点 Key 加互斥锁
2. 热点数据提前续期

### 3. 缓存雪崩

**问题**：大量 Key 同时过期。

**方案**：

1. TTL 加随机值
2. 做多级缓存或降级保护

---

## 🧪 建议练习

1. 给用户详情查询增加 `@Cacheable`
2. 给用户更新、删除增加 `@CacheEvict`
3. 用 Redis 保存一次验证码并设置 5 分钟过期
4. 统计缓存命中率，观察 Redis 与 MySQL 的访问变化

---

## 📝 最佳实践

- 不要缓存所有分页结果，优先缓存“高命中、参数少”的数据
- Key 要有统一前缀，避免跨业务污染
- TTL 必须配置，避免永久脏数据
- 写操作后及时删除或刷新缓存
- 缓存是性能优化层，不应成为唯一数据来源

---

## 📝 学习总结

- Redis 不是“有就加”，而是针对热点读取做有目的优化
- Spring Cache 适合快速落地，但复杂场景仍要手写 RedisTemplate / StringRedisTemplate
- 当前项目最适合从“用户详情缓存 + 权限缓存 + 验证码缓存”开始

---

## 📚 参考资料

- [Spring Cache Reference](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
- [Redis 官方文档](https://redis.io/docs/)

