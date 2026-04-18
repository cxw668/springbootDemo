# 3.4 定时任务 - 学习与实践

> 对应学习路线：阶段 3.4

---

## 📚 学习目标

1. 掌握 `@Scheduled` 的基本用法
2. 理解固定频率、固定延迟、Cron 表达式的区别
3. 了解 Quartz 的适用场景
4. 能为当前项目设计并实现实用定时任务
5. 理解定时任务的并发控制、幂等和异常处理

---

## 🎯 核心知识点

### 1. `@Scheduled` 常用模式

| 注解参数 | 说明 |
|----------|------|
| `fixedRate` | 按开始时间间隔执行 |
| `fixedDelay` | 上一次执行结束后再等待 |
| `cron` | 按 Cron 表达式执行 |
| `initialDelay` | 应用启动后延迟第一次执行 |

### 2. 三种调度方式区别

| 方式 | 特点 | 场景 |
|------|------|------|
| `fixedRate` | 关注执行频率 | 周期采集 |
| `fixedDelay` | 避免任务重叠 | 数据同步 |
| `cron` | 表达能力强 | 每天报表、凌晨清理 |

### 3. 基本执行流程

```text
Spring 启动
  -> 扫描 `@Scheduled`
  -> 注册到任务调度器
  -> 到达触发时间
  -> 执行任务方法
  -> 记录日志 / 处理异常
```

---

## 🔧 当前项目落地建议

### 适合当前项目的定时任务

| 任务 | 说明 |
|------|------|
| 清理过期上传文件 | 结合 3.5 文件上传主题 |
| 清理过期 Token / 验证码 | 结合 3.1、3.2 |
| 每日统计用户数量 | 生成简单日报 |
| 定期同步缓存热点数据 | 预热缓存 |

### 推荐从简单任务开始

先做：

1. 每天凌晨清理临时文件
2. 每小时统计一次用户总数并写日志

---

## 🧩 实践步骤

### 步骤 1：启用调度

```java
@EnableScheduling
@SpringBootApplication
public class SpringbootDemoApplication {
}
```

### 步骤 2：创建任务类

```java
@Slf4j
@Component
public class FileCleanupJob {

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredFiles() {
        log.info("开始清理过期文件");
    }
}
```

### 步骤 3：使用配置驱动 Cron

```java
@Scheduled(cron = "${app.job.file-cleanup-cron}")
public void cleanExpiredFiles() {
    ...
}
```

```yaml
app:
  job:
    file-cleanup-cron: "0 0 2 * * ?"
```

### 步骤 4：控制并发与耗时

如果任务耗时较长，建议自定义线程池：

```java
@Configuration
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("job-");
        return scheduler;
    }
}
```

---

## 🆚 什么时候用 Quartz

| 场景 | `@Scheduled` | Quartz |
|------|--------------|--------|
| 单体项目简单定时任务 | 适合 | 不必上 |
| 任务时间固定 | 适合 | 也可 |
| 动态新增/暂停/恢复任务 | 一般 | 更适合 |
| 持久化任务状态 | 不支持 | 支持 |
| 集群调度 | 较弱 | 更成熟 |

当前项目建议先掌握 `@Scheduled`，需要“任务后台管理”时再引入 Quartz。

---

## 🧪 建议练习

1. 每天凌晨清理 7 天前的临时上传文件
2. 每小时输出一次系统内用户数量
3. 给任务日志加 requestId 之外的 jobId 或 taskName
4. 任务异常时记录错误日志并告警

---

## ⚠️ 常见问题

### 1. 任务没有执行

通常是以下原因：

1. 忘记加 `@EnableScheduling`
2. 任务类没有被 Spring 扫描到
3. Cron 表达式写错

### 2. 任务重复执行

在多实例部署下，同一个 `@Scheduled` 任务可能每个实例都执行一次。

解决思路：

- 单机部署可以直接使用
- 集群环境需加分布式锁，如 Redis / ShedLock

### 3. 任务异常后静默失败

任务代码必须主动记录异常日志，必要时结合告警系统。

---

## 📝 学习总结

- 定时任务的重点不只是“定时执行”，而是**可观测、可配置、可恢复**
- `@Scheduled` 足以覆盖当前项目大部分需求
- 与文件上传、缓存、安全能力结合后，定时任务会更有真实业务价值

---

## 📚 参考资料

- [Spring Scheduling Reference](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Quartz Scheduler](https://www.quartz-scheduler.org/documentation/)

