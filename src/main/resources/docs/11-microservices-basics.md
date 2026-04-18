# 3.6 微服务基础 - 学习与实践

> 对应学习路线：阶段 3.6

---

## 📚 学习目标

1. 理解单体应用与微服务的边界差异
2. 熟悉服务注册发现、配置中心、网关、服务调用等基础组件
3. 了解 Spring Cloud / Spring Cloud Alibaba 常见组件职责
4. 能基于当前项目设计一个合理的拆分方案
5. 知道微服务不是目的，而是复杂度上升后的治理手段

---

## 🎯 核心知识点

### 1. 单体 vs 微服务

| 维度 | 单体 | 微服务 |
|------|------|--------|
| 部署 | 一个应用整体部署 | 多个服务独立部署 |
| 开发 | 上手简单 | 协作与治理复杂 |
| 调用 | 进程内调用 | 网络调用 |
| 扩展 | 整体扩容 | 按服务扩容 |

### 2. 微服务核心组件

| 组件 | 作用 | 常见实现 |
|------|------|----------|
| 注册中心 | 服务注册与发现 | Nacos、Eureka |
| 配置中心 | 统一配置管理 | Nacos Config、Apollo |
| 网关 | 统一入口、鉴权、限流 | Spring Cloud Gateway |
| 服务调用 | 服务间通信 | OpenFeign、RestClient |
| 熔断限流 | 提升稳定性 | Resilience4j、Sentinel |

### 3. 基本调用链

```text
Client
  -> Gateway
  -> user-service
  -> file-service
  -> Redis / MySQL / MQ
```

---

## 🔧 当前项目拆分建议

### 推荐的学习型拆分方案

当前项目不适合一次拆太细，建议拆成 3 个核心服务：

| 服务 | 主要职责 |
|------|----------|
| `gateway-service` | 统一入口、路由转发、鉴权 |
| `user-service` | 用户 CRUD、权限、登录 |
| `file-service` | 文件上传、文件元数据管理 |

### 为什么这样拆

1. 用户与文件已经具备相对独立的业务边界
2. 网关可承接统一鉴权、日志、跨域等横切能力
3. 便于后续继续接入缓存、消息队列、监控

---

## 🧩 实践步骤

### 步骤 1：引入注册中心和配置中心

推荐优先学习 Spring Cloud Alibaba + Nacos：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

### 步骤 2：给每个服务配置服务名

```yaml
spring:
  application:
    name: user-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

### 步骤 3：搭建网关

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
```

### 步骤 4：服务间调用

```java
@FeignClient(name = "file-service")
public interface FileClient {

    @GetMapping("/internal/files/{id}")
    FileInfo getFile(@PathVariable Long id);
}
```

### 步骤 5：治理基础能力

服务拆分后至少要同步建设：

1. 统一日志链路 ID
2. 统一错误码和响应结构
3. 统一鉴权策略
4. 统一配置管理

---

## 🧱 拆分前必须考虑的问题

### 1. 服务边界是否清晰

不要因为“学微服务”就把一个简单模块拆成很多服务。

### 2. 数据如何管理

原则：**每个服务尽量拥有自己的数据边界**，避免跨服务直接共用数据库表。

### 3. 分布式问题是否能承受

拆分后会引入：

- 网络抖动
- 服务调用失败
- 分布式事务
- 配置漂移
- 排查链路变长

---

## 🧪 建议练习

1. 把当前项目拆出 `gateway-service` 与 `user-service`
2. 使用 Nacos 完成服务注册发现
3. 使用 Gateway 路由 `/api/users/**` 请求
4. 使用 OpenFeign 模拟一次服务间调用

---

## ⚠️ 常见误区

### 1. 项目一开始就上微服务

如果业务简单、团队小、部署环境少，单体往往更高效。

### 2. 只拆服务，不做治理

没有网关、配置中心、监控、日志链路的微服务，很快会失控。

### 3. 继续共用一套数据库

短期能跑，长期会让服务边界失效。

---

## 📝 学习总结

- 微服务解决的是“大系统协作和治理”问题，不是基础 CRUD 项目的必选项
- 当前项目更适合把微服务作为**演进式学习主题**
- 最好的实践顺序是：**先单体做扎实，再拆网关，再拆核心服务**

---

## 📚 参考资料

- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/)
- [Nacos 官方文档](https://nacos.io/docs/latest/overview/)

