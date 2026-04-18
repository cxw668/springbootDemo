# 3.7 监控与运维 - 学习与实践

> 对应学习路线：阶段 3.7

---

## 📚 学习目标

1. 理解应用监控、日志、告警的基本关系
2. 掌握 Spring Boot Actuator 的常见端点
3. 了解 Micrometer、Prometheus、Grafana 的职责分工
4. 能为当前项目接入健康检查和基础指标采集
5. 能初步建立“可观测性”思维

---

## 🎯 核心知识点

### 1. 什么是可观测性

应用上线后，需要能回答三类问题：

1. **是否存活**：应用健康吗？
2. **是否变慢**：接口延迟高吗？
3. **是否出错**：异常率是否上升？

### 2. 监控体系分层

| 层次 | 工具 | 作用 |
|------|------|------|
| 健康检查 | Actuator | 暴露应用状态 |
| 指标采集 | Micrometer | 统一指标模型 |
| 时序存储 | Prometheus | 抓取并保存指标 |
| 图表展示 | Grafana | 可视化看板 |
| 日志检索 | ELK / Loki | 排查详细问题 |

### 3. 常用 Actuator 端点

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康状态 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 指标列表 |
| `/actuator/prometheus` | Prometheus 抓取入口 |
| `/actuator/env` | 环境变量与配置 |

---

## 🔧 当前项目落地建议

### 第一阶段目标

先实现最小可用监控：

1. 接入 Actuator
2. 开放 `health`、`info`、`metrics`、`prometheus`
3. 让 Prometheus 抓取应用指标
4. 在 Grafana 里看 JVM、HTTP、数据库连接池指标

### 重点关注指标

| 类型 | 示例 |
|------|------|
| JVM | 内存、GC、线程数 |
| HTTP | 请求总量、状态码、平均耗时 |
| 数据源 | 连接池活跃数、等待数 |
| 业务 | 用户创建次数、文件上传次数 |

---

## 🧩 实践步骤

### 步骤 1：引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 步骤 2：开放端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 步骤 3：查看健康检查

访问：

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/prometheus
```

### 步骤 4：增加自定义业务指标

```java
Counter uploadCounter = Counter.builder("biz.file.upload.count")
        .description("文件上传次数")
        .register(meterRegistry);

uploadCounter.increment();
```

### 步骤 5：Prometheus 抓取配置

```yaml
scrape_configs:
  - job_name: 'springboot-demo'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

---

## 🧪 建议练习

1. 接入 Actuator 并确认健康检查正常
2. 在 Grafana 展示 JVM 内存和接口耗时
3. 为“文件上传成功次数”增加自定义 Counter
4. 为“用户创建耗时”增加 Timer

---

## ⚠️ 安全与运维注意事项

### 1. 不要在生产环境随意暴露所有端点

像 `/actuator/env`、`/actuator/beans` 这类端点可能暴露敏感信息，应按需开放。

### 2. 健康检查要区分存活和就绪

容器环境里通常需要：

- Liveness：应用进程是否活着
- Readiness：应用是否已经可以接流量

### 3. 指标要服务于排障

不要为了“看起来专业”堆很多图表，关键是能支持定位问题。

---

## 📝 学习总结

- Actuator 解决“应用是否正常”的问题
- Prometheus 解决“指标如何采集与存储”的问题
- Grafana 解决“如何直观看出异常趋势”的问题
- 当前项目最适合从健康检查、JVM 指标、HTTP 指标、自定义业务指标四类开始

---

## 📚 参考资料

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html)
- [Micrometer Docs](https://micrometer.io/docs)
- [Prometheus Docs](https://prometheus.io/docs/introduction/overview/)
- [Grafana Docs](https://grafana.com/docs/)
