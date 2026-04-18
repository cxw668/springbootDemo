# 3.3 消息队列 - 学习与实践

> 对应学习路线：阶段 3.3

---

## 📚 学习目标

1. 理解消息队列在解耦、削峰、异步处理中的作用
2. 掌握 RabbitMQ 核心模型：Exchange、Queue、Binding、Routing Key
3. 理解消息确认、重试、死信队列等可靠性机制
4. 能为当前项目设计一个异步处理场景
5. 知道 RabbitMQ 与 Kafka 的定位差异

---

## 项目消息队列工作流程

触发：业务代码（如注册成功）调用 DomainEventPublisher.publish(event)。
路由：RabbitMqDomainEventPublisher 根据事件类型找到对应的 Exchange 和 Routing Key。
传输：消息进入 RabbitMQ 队列（配置了 DLQ 死信队列以防丢失）。
消费：listener 监听到消息。
去重：调用 MessageDedupService.tryStartProcessing() 检查 Redis。
执行：如果未处理过，则执行后续逻辑；否则直接丢弃。

---

## 🎯 核心知识点

### 1. 为什么需要消息队列

典型场景：

```text
用户注册成功
  -> 发欢迎邮件
  -> 记录审计日志
  -> 初始化积分
```

如果都在主流程同步执行：

- 接口响应慢
- 任一子任务失败都会拖累主流程

引入 MQ 后：

```text
主业务提交成功
  -> 发布消息
  -> 多个消费者异步处理
```

### 2. RabbitMQ 核心对象

| 对象 | 作用 |
|------|------|
| Producer | 生产消息 |
| Exchange | 接收消息并路由 |
| Queue | 存储消息 |
| Consumer | 消费消息 |
| Routing Key | 路由规则 |

### 3. 常见交换机类型

| 类型 | 场景 |
|------|------|
| Direct | 精确路由，最常用 |
| Topic | 模糊匹配，适合业务事件 |
| Fanout | 广播消息 |
| Headers | 较少使用 |

---

## 🔧 当前项目落地建议

### 推荐练手场景

| 场景 | 是否适合 MQ | 说明 |
|------|-------------|------|
| 用户注册后发送欢迎通知 | 适合 | 典型异步任务 |
| 文件上传后生成缩略图 | 适合 | 耗时任务异步化 |
| 导出报表 | 适合 | 后台异步生成 |
| 普通 CRUD 查库 | 不适合 | 不能为了 MQ 而 MQ |

### RabbitMQ 与 Kafka 选择

| 项目阶段 | 推荐 |
|----------|------|
| 当前单体学习项目 | RabbitMQ |
| 高吞吐日志/埋点流式处理 | Kafka |

---

## 🧩 本次实践做了什么

这次在当前项目里实际落了两条事件链路：

| 事件 | 触发位置 | 异步处理内容 |
|------|----------|--------------|
| `user.registered` | `AuthController#register` | 欢迎通知、注册审计日志 |
| `file.uploaded` | `FileController#uploadAvatar` / `uploadFile` | 文件元数据提取、缩略图/后处理任务 |

同时补了 4 个工程化能力：

1. **Topic Exchange + 业务 Routing Key**
2. **DLQ（死信队列）**
3. **Redis 幂等去重**
4. **测试环境关闭 MQ，避免依赖外部 Broker**

---

## 🧩 实现结构

### 1. 生产者

当前项目新增了 `DomainEventPublisher` 抽象：

- `RabbitMqDomainEventPublisher`：MQ 开启时，真正发布事件
- `NoOpDomainEventPublisher`：MQ 未开启时，安全降级，不影响主流程

这样控制器不需要直接依赖 `RabbitTemplate`，业务层和消息基础设施是解耦的。

### 2. 交换机与队列

本次实践使用：

| 类型 | 名称 | 用途 |
|------|------|------|
| Topic Exchange | `app.event.exchange` | 业务事件主交换机 |
| Direct Exchange | `app.event.dlx` | 死信交换机 |
| Queue | `user.registered.queue` | 用户注册事件消费 |
| Queue | `file.uploaded.queue` | 文件上传事件消费 |
| Queue | `user.registered.dlq` | 注册事件死信队列 |
| Queue | `file.uploaded.dlq` | 文件上传死信队列 |

对应路由键：

```text
user.registered
file.uploaded
user.registered.dlq
file.uploaded.dlq
```

### 3. 消费者

当前项目新增两个监听器：

| 监听器 | 主队列 | 作用 |
|--------|--------|------|
| `UserRegisteredEventListener` | `user.registered.queue` | 异步欢迎通知 + 审计日志 |
| `FileUploadedEventListener` | `file.uploaded.queue` | 异步文件后处理 |

每个监听器都额外监听对应的 DLQ，用于记录失败消息。

### 4. 幂等去重

本次直接结合前面已经接入的 Redis，实现了简单但实用的幂等消费：

```text
mq:processed:{eventId}
```

消费者第一次处理消息时：

1. 先写入一个 `PROCESSING` 状态
2. 处理成功后改成 `DONE`
3. 处理失败则清理处理标记，允许 Broker 重投

这适合当前学习项目，也贴近生产里常见的“全局唯一消息 ID + 幂等键”方案。

---

## 🧩 实践步骤

### 步骤 1：引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 步骤 2：配置连接

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:127.0.0.1}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    publisher-confirm-type: correlated
    publisher-returns: true
```

同时在项目里通过配置开关控制是否启用 MQ：

```yaml
app:
  messaging:
    enabled: true
```

### 步骤 3：声明交换机与队列

```java
QueueBuilder.durable("user.registered.queue")
        .deadLetterExchange("app.event.dlx")
        .deadLetterRoutingKey("user.registered.dlq")
        .build();
```

这里的关键不是“声明一个队列”，而是**从一开始就把 DLQ 配上**，避免失败消息反复重试造成毒性循环。

### 步骤 4：发送消息

```java
rabbitTemplate.convertAndSend(
        "app.event.exchange",
        "user.registered",
        event
);
```

### 步骤 5：消费消息

```java
@RabbitListener(queues = "user.registered.queue")
public void handleUserRegistered(UserRegisteredEvent event) {
    if (messageDedupService.isProcessed(event.getEventId())) {
        log.warn("重复消息，跳过: {}", event.getEventId());
        return;
    }
    if (!messageDedupService.tryStartProcessing(event.getEventId())) {
        return;
    }
    log.info("异步发送欢迎通知: {}", event.getUsername());
    messageDedupService.markProcessed(event.getEventId());
}
```

---

## 🛡️ 应用场景怎么映射到当前项目

| 场景 | 当前项目可落地方式 |
|------|------------------|
| 异步任务（邮件、图片/视频处理） | 注册后欢迎通知；文件上传后生成缩略图/提取元数据 |
| 削峰填谷（缓冲高峰流量） | 高峰期把注册后续动作、文件后处理放入队列 |
| 微服务解耦与事件驱动 | User 模块发布事件，通知/审计/文件处理模块独立消费 |
| 实时流/日志采集与分析 | 当前项目文档中推荐后续升级到 Kafka |
| 分布式任务队列/调度 | 导出报表、批量同步任务可由消费者异步执行 |
| 延迟/定时任务（delay queue） | 过期清理、延后通知、超时自动取消等后续扩展 |

---

## 🛡️ 常见问题场景与解决方案

### 1. 消息丢失

**问题来源：**

1. 生产者发送失败
2. Broker 未持久化
3. 消费前节点异常

**解决方案：**

- 队列/交换机/消息持久化
- 生产者确认（publisher confirm）
- 多副本/镜像队列
- 必要时记录业务发送日志，做补发

### 2. 重复消费

**问题来源：** 网络重试、消费者重启、Broker 重投递。

**解决方案：**

- 幂等消费
- 去重表 / Redis 幂等键
- 使用全局唯一消息 ID

本次项目已经落了 Redis 去重。

### 3. 顺序保证

**问题来源：** 多消费者并发时，同一业务键消息顺序可能乱掉。

**解决方案：**

- 按业务键分区 / 分路由
- 同一业务键单线程消费
- 或增加序号校验

RabbitMQ 不擅长大规模全局有序，通常只保证“单队列 + 单消费者”级别的顺序。

### 4. 毒性消息

**问题来源：** 某条消息无论重试多少次都会失败。

**解决方案：**

- 死信队列（DLQ）
- 人工排查
- 自动降级或补偿处理

本次项目已经给两个主队列都配置了 DLQ。

### 5. 背压 / 延迟升高

**问题来源：** 生产速度大于消费速度。

**解决方案：**

- 限流
- 批处理
- 消费者扩容
- 调整 prefetch 和并发
- 拆分热点队列

### 6. 事务与一致性

**问题来源：** 数据库成功了，消息没发出去；或者消息发出去了，业务没落库。

**解决方案：**

- 端到端幂等
- 补偿事务
- Outbox 模式
- Kafka Transactions（特定场景）

当前项目是学习型实现，先从“主业务成功后发布事件 + 消费端幂等”入手最合适。

### 7. 可观测性

**问题来源：** 消息卡住、堆积、失败时不容易定位。

**解决方案：**

- 指标
- 结构化日志
- 链路追踪
- 告警

当前项目里至少要关注：

1. 队列积压数
2. 消费失败数
3. DLQ 消息数量
4. 事件 ID 对应的完整处理日志

---

## 🧪 如何手动验证

### 1. 验证注册事件

调用 `/auth/register` 后，观察日志是否出现：

```text
已发布 user.registered 事件
异步发送欢迎通知
异步记录注册审计日志
```

### 2. 验证文件上传事件

调用 `/api/files/upload` 或 `/api/files/avatar` 后，观察日志是否出现：

```text
已发布 file.uploaded 事件
异步处理文件上传事件
执行后处理任务: 提取元数据 / 生成缩略图 / 调用后续处理流水线
```

### 3. 验证死信队列

为了便于本地演示，项目里留了两个显式的失败入口：

1. 注册用户名以 `fail-mq-` 开头，触发 `user.registered` 消费失败
2. 上传文件时把 `subDir=simulate-fail`，触发 `file.uploaded` 消费失败

失败后应能看到对应 DLQ 日志。

---

## 🧪 建议练习

1. 给注册事件接入真实邮件服务
2. 给文件上传事件接入图片缩略图生成
3. 增加消费者失败告警
4. 为消息发布和消费增加监控指标

---

## 🚀 后续支持更大数据量时怎么选

当系统从“业务异步”升级到“大吞吐流式数据”时，RabbitMQ 往往不是终点。

### 1. RabbitMQ 适合什么阶段

- 中小规模业务事件
- 接口异步化
- 延迟队列 / 死信 / 任务编排
- 对消息路由灵活性要求高

### 2. Kafka 适合什么阶段

- 海量日志采集
- 实时流处理
- 埋点、行为流、监控数据
- 高吞吐、可回放、多消费者组

### 3. 如果继续扩大规模，还可以看哪些选项

| 方案 | 优势 | 更适合的场景 |
|------|------|--------------|
| RabbitMQ | 路由灵活、DLQ/延迟队列成熟、业务异步友好 | 业务通知、任务编排、订单/用户事件 |
| Kafka | 吞吐高、可回放、生态成熟 | 日志流、埋点流、实时分析 |
| RocketMQ | 顺序消息、事务消息能力强 | 交易链路、金融/电商业务事件 |
| Pulsar | 存算分离、租户隔离能力强 | 更复杂的多租户事件平台、云原生消息平台 |

### 4. 选型建议

| 业务特征 | 推荐 |
|----------|------|
| 用户注册、订单通知、文件后处理 | RabbitMQ |
| 日志流、行为流、IoT 数据流 | Kafka |
| 海量可回放事件流 | Kafka |
| 强事务消息、业务顺序要求高 | RocketMQ |
| 强路由、灵活 DLQ、延迟任务 | RabbitMQ |

### 5. 当前项目的演进路线

推荐按这个顺序走：

1. **现在**：RabbitMQ 做业务事件异步化
2. **下一步**：加监控、告警、消费者并发和失败补偿
3. **数据量更大时**：把日志采集、行为事件流拆到 Kafka
4. **事务/顺序要求变强时**：评估 RocketMQ
5. **再往后**：RabbitMQ 保留业务通知，Kafka 或 RocketMQ 承接高吞吐主链路

这也是很多企业项目的常见组合：**RabbitMQ 处理业务任务，Kafka 处理数据流平台能力**；如果交易链路需要更强事务消息能力，再补充 RocketMQ。

---

## 📝 学习总结

- MQ 的核心价值是解耦、异步、削峰和事件驱动
- 当前项目最合适的 RabbitMQ 落地点是“注册事件 + 文件上传后处理”
- 真正的工程难点始终是：**丢失、重复、顺序、毒性消息、积压和可观测性**
- 当系统进入更高吞吐阶段，RabbitMQ 和 Kafka 往往不是二选一，而是分工协作

---

## 📚 参考资料

- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/tutorials.html)

