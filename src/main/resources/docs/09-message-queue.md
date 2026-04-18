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
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
```

### 步骤 3：声明交换机与队列

```java
@Bean
public DirectExchange userExchange() {
    return new DirectExchange("user.exchange");
}

@Bean
public Queue userRegisterQueue() {
    return new Queue("user.register.queue");
}

@Bean
public Binding registerBinding() {
    return BindingBuilder.bind(userRegisterQueue())
            .to(userExchange())
            .with("user.register");
}
```

### 步骤 4：发送消息

```java
rabbitTemplate.convertAndSend(
        "user.exchange",
        "user.register",
        new UserRegisterMessage(userId, username)
);
```

### 步骤 5：消费消息

```java
@RabbitListener(queues = "user.register.queue")
public void handleRegister(UserRegisterMessage message) {
    log.info("处理注册后任务: {}", message);
}
```

---

## 🛡️ 可靠性设计

### 1. 消息确认

- 生产者确认：消息是否成功到达 Broker
- 消费者确认：消息是否成功处理完成

### 2. 重试机制

消费失败时不要无限重试，建议：

1. 有限次数重试
2. 重试失败后转入死信队列
3. 人工排查死信

### 3. 幂等性

同一条消息可能被重复消费，消费者应保证幂等。

常见做法：

- 基于业务唯一键判重
- 基于消息 ID 记录处理状态

---

## 🧪 建议练习

1. 用户创建成功后发送一条“注册成功”消息
2. 消费者异步记录一条操作日志
3. 模拟消费者异常，观察是否会重复消费
4. 增加死信队列，用于保存处理失败的消息

---

## ⚠️ 常见问题

### 1. 消息发出去了但消费者收不到

优先检查：

1. Exchange、Queue、Binding 是否一致
2. Routing Key 是否匹配
3. 消费者监听的队列名是否正确

### 2. 消息重复消费

不要假设 MQ “只会投递一次”，而要让消费者做到“重复处理也安全”。

### 3. 把 MQ 当事务总线

MQ 适合最终一致性和异步处理，不适合代替本地事务。

---

## 📝 学习总结

- MQ 的核心价值是解耦与异步，而不是“让架构更高级”
- RabbitMQ 更适合当前项目做业务通知、文件后处理、审计日志等场景
- 真正难点不在发消息，而在**可靠性、幂等性、失败补偿**

---

## 📚 参考资料

- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/tutorials.html)

