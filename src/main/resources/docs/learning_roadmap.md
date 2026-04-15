# Spring Boot → Spring AI 学习路线

基于当前项目（Spring Boot 3 + MyBatis-Plus + MySQL），从基础到 Spring AI 的分阶段学习路线。

---

## 阶段 1：夯实基础 ✅（已在进行中）

| 序号 | 主题 | 核心内容 | 实践目标 |
|------|------|---------|---------|
| 1.1 | Spring IoC/DI | Bean 管理、依赖注入、生命周期 | 手写配置类，理解 `@Component` vs `@Bean` |
| 1.2 | Spring MVC | 请求映射、参数绑定、返回值处理 | 熟练使用 `@RestController`、`@RequestBody` |
| 1.3 | 数据访问 | JDBC、MyBatis-Plus、事务管理 | 完成 CRUD + 分页 + 条件查询 |
| 1.4 | 自动装配 | `@SpringBootApplication`、starter 机制 | 理解 starter 原理，能自定义 starter |
| 1.5 | API 测试 | HTTP Client 文件、Postman | 掌握请求链、环境变量、断言 |

**当前项目覆盖度**：约 80%（已完成基础 CRUD、MyBatis-Plus 插件、API 测试）

---

## 阶段 2：核心进阶 🔄

| 序号 | 主题 | 核心内容 | 实践目标 |
|------|------|---------|---------|
| 2.1 | 统一响应格式 | 封装 Result<T>、全局异常处理 | 所有接口返回统一格式，处理业务异常 |
| 2.2 | 参数校验 | `@Valid`、`@NotNull`、自定义校验器 | 请求参数自动校验，返回友好错误信息 |
| 2.3 | 拦截器/过滤器 | HandlerInterceptor、Filter、CORS | 实现请求日志、跨域处理、简单鉴权 |
| 2.4 | 日志管理 | SLF4J、Logback 配置、MDC | 按级别输出，请求链路追踪 |
| 2.5 | 单元测试进阶 | Mockito、MockMvc、H2 | 独立测试 Controller/Service 层 |
| 2.6 | 接口文档 | SpringDoc / Swagger | 自动生成 API 文档，支持在线调试 |
| 2.7 | 配置管理 | `@ConfigurationProperties`、Profile、Nacos | 多环境配置、动态刷新 |

**里程碑**：将当前项目重构为生产级代码风格

---

## 阶段 3：高级特性 📚

| 序号 | 主题 | 核心内容 | 实践目标 |
|------|------|---------|---------|
| 3.1 | 安全框架 | Spring Security / Sa-Token | JWT 登录、权限控制、RBAC 模型 |
| 3.2 | 缓存 | Redis、`@Cacheable`、缓存策略 | 热点数据缓存、缓存穿透/雪崩处理 |
| 3.3 | 消息队列 | RabbitMQ / Kafka | 异步处理、事件驱动架构 |
| 3.4 | 定时任务 | `@Scheduled`、Quartz | 数据同步、定时报表 |
| 3.5 | 文件上传 | MultipartFile、OSS 存储 | 头像上传、大文件分片 |
| 3.6 | 微服务基础 | Spring Cloud、Gateway、Nacos | 服务注册发现、负载均衡 |
| 3.7 | 监控与运维 | Actuator、Prometheus、Grafana | 健康检查、性能监控 |

---

## 阶段 4：Spring AI 🤖

| 序号 | 主题 | 核心内容 | 实践目标 |
|------|------|---------|---------|
| 4.1 | AI 基础概念 | Prompt、模型、Token、Embedding | 理解 LLM 工作原理，熟悉 OpenAI API |
| 4.2 | Spring AI 入门 | ChatModel、Streaming、工具调用 | 接入 OpenAI/Claude，完成简单对话 |
| 4.3 | Prompt 工程 | Prompt Template、输出格式化、System Message | 设计高质量 Prompt，控制输出格式 |
| 4.4 | 向量数据库 | EmbeddingModel、Vector Store、相似度搜索 | 构建 RAG（检索增强生成）应用 |
| 4.5 | Function Calling | 工具定义、参数提取、多工具路由 | 让 AI 调用项目内部 API |
| 4.6 | 文档问答 | PDF/Word 解析、分块、向量化 | 基于本地文档的智能问答 |
| 4.7 | Agent 模式 | ReAct、规划-执行-验证循环 | 构建能自主规划任务的 AI Agent |

**里程碑**：为当前项目接入 AI 能力（智能客服、文档分析、代码助手等）

---

## 学习建议

### 📌 每个阶段的学习方法
1. **看文档** — 官方文档 > 博客 > 视频教程
2. **做笔记** — 记录核心概念、常见坑、代码片段
3. **写代码** — 每个知识点至少写一个 demo
4. **复习** — 每周末回顾本周内容

### 📌 推荐资源
| 类型 | 推荐 |
|------|------|
| 官方文档 | docs.spring.io（Spring Boot / Spring AI） |
| 视频 | 官方 YouTube 频道、B 站优质教程 |
| 书籍 | 《Spring Boot 实战》、《Spring AI 实战》 |
| 社区 | Stack Overflow、GitHub Issues、掘金 |

### 📌 时间分配建议
| 阶段 | 预计周期 | 每日投入 |
|------|---------|---------|
| 阶段 1 | 1-2 周 | 2-3 小时 |
| 阶段 2 | 2-3 周 | 2-3 小时 |
| 阶段 3 | 3-4 周 | 2-3 小时 |
| 阶段 4 | 2-3 周 | 2-3 小时 |

---

## 下一步行动

当前阶段 1 已基本完成，建议：
1. ✅ 先完善阶段 1 剩余部分（单元测试修复并跑通）
2. 🔄 开始阶段 2.1（统一响应格式 + 全局异常处理）
3. 📝 每完成一个主题，更新本文档标注进度
