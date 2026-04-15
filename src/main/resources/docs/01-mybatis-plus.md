# MyBatis-Plus 学习清单

- [X] 先搞清定位：MyBatis-Plus 是 MyBatis 增强工具，只做增强不做改变，核心价值是减少单表 CRUD 和条件查询样板代码。
- [X] 完成安装：确认 JDK 8+；Spring Boot 2 用 `mybatis-plus-boot-starter`，Spring Boot 3 用 `mybatis-plus-spring-boot3-starter`。
- [X] 记住版本点：3.5.9+ 起，分页等插件相关能力需要按需单独引入 `mybatis-plus-jsqlparser`（按需引入）。
- [X] 跑通快速开始：配置数据源、`@MapperScan`、实体类、Mapper 接口，完成 `insert`、`selectById`、`updateById`、`deleteById`。
- [X] 掌握实体映射注解：`@TableName`、`@TableId`、`@TableField`。
- [X] 掌握主键策略：重点看 `IdType.AUTO`、`IdType.ASSIGN_ID`、`IdType.ASSIGN_UUID`、`IdType.INPUT`。
- [X] 熟悉 `BaseMapper`：重点掌握 `insert`、`deleteById`、`updateById`、`selectById`、`selectList`、`selectCount`、`selectPage`。
- [X] 熟悉 `IService`：重点掌握 `save`、`saveBatch`、`saveOrUpdate`、`remove`、`update`、`get`、`list`、`page`、`count`。
- [X] 把条件构造器学透：优先使用 `LambdaQueryWrapper`、`LambdaUpdateWrapper`，避免手写字段名。
- [X] 条件构造器重点方法：`eq`、`ne`、`gt`、`ge`、`lt`、`le`、`like`、`in`、`between`、`orderBy`、`select`、`and`、`or`、`nested`。
- [X] 学会分页：配置 `MybatisPlusInterceptor` + `PaginationInnerInterceptor`，掌握 `Page` 和 `IPage` 的用法。
- [X] 学会自动填充：使用 `@TableField(fill = ...)` 配合 `MetaObjectHandler` 处理创建时间、更新时间、创建人、更新人。
- [X] 学会逻辑删除：掌握 `@TableLogic`，明确未删除值和已删除值，也要知道可通过全局配置统一设置。
- [X] 学会乐观锁：掌握 `@Version` + `OptimisticLockerInnerInterceptor`，理解并发更新冲突的处理方式。
- [X] 了解全局配置：重点看 `GlobalConfig.DbConfig` 中的 `idType`、`tablePrefix`、`tableUnderline`、`logicDeleteField`、`insertStrategy`、`updateStrategy`、`whereStrategy`。
- [X] 了解常用注解补充：`@EnumValue`、`@KeySequence`、`@OrderBy`、`@InterceptorIgnore`。
- [X] 了解插件体系：`MybatisPlusInterceptor` 是统一入口，常用插件有分页、多租户、动态表名、乐观锁、非法 SQL 拦截、防全表更新删除。
- [X] 记住插件顺序：多租户/动态表名 -> 分页/乐观锁 -> 非法 SQL 拦截/防全表更新删除。
- [X] 按需学习高级能力：多租户插件、动态表名插件、数据权限插件、SimpleQuery、Db Kit、Chain、ActiveRecord。
- [X] 学代码生成器：3.5.1+ 优先用新代码生成器 `FastAutoGenerator`，至少会配数据源、包名、策略、逻辑删除、乐观锁、字段填充。
- [X] 建立使用习惯：单表 CRUD 优先用 MP，复杂联表和特殊 SQL 再自定义 Mapper XML 或注解 SQL。
- [X] 最后做一遍实战：独立完成一个模块的实体映射、基础 CRUD、条件查询、分页、逻辑删除、自动填充、乐观锁、代码生成。

---

## 1 核心定位（回顾）

MyBatis-Plus（MP）是对 MyBatis 的非侵入式增强工具。核心目标是减少单表 CRUD 和通用条件查询的样板代码，让开发者把精力放在业务逻辑上。

要点：简单、可替代现有 MyBatis 代码、按需引入插件，不改变 MyBatis 原有执行流程。

---

## 2 快速开始（最小可运行示例）

1) 依赖（Maven 示例）：

```xml
<!-- Spring Boot 2.x -->
<dependency>
   <groupId>com.baomidou</groupId>
   <artifactId>mybatis-plus-boot-starter</artifactId>
   <version>3.5.x</version>
</dependency>

<!-- Spring Boot 3.x -->
<dependency>
   <groupId>com.baomidou</groupId>
   <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
   <version>3.5.x</version>
</dependency>
```

注意：若使用 3.5.9+ 且涉及复杂 SQL 解析，按需引入 `mybatis-plus-jsqlparser`。

2) 配置数据源与扫描：

application.yml 示例：

```yaml
spring:
   datasource:
      url: jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
   configuration:
      map-underscore-to-camel-case: true
```

主启动类或配置类上添加：

```java
@SpringBootApplication
@MapperScan("com.example.mapper")
public class DemoApplication { ... }
```

3) 最小配置 Bean（推荐）：

```java
@Configuration
public class MybatisPlusConfig {
      @Bean
      public MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            return interceptor;
      }
}
```

4) 最小实体/Mapper/Service：

```java
@Data
@TableName("user")
public class User {
   @TableId(type = IdType.ASSIGN_ID)
   private Long id;
   private String name;
   private Integer age;
   @TableField(fill = FieldFill.INSERT)
   private LocalDateTime createTime;
   @TableField(fill = FieldFill.INSERT_UPDATE)
   private LocalDateTime updateTime;
   @TableLogic
   private Integer deleted;
   @Version
   private Integer version;
}

public interface UserMapper extends BaseMapper<User> {}

public interface UserService extends IService<User> {}

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}
```

测试 CRUD：在 Service 中调用 `save`、`getById`、`updateById`、`removeById`。

---

## 3 实体映射注解说明

- `@TableName("table_name")`：指定表名
- `@TableId(type = IdType.ASSIGN_ID)`：主键策略
- `@TableField(value = "db_col", fill = FieldFill.INSERT_UPDATE, exist = true/false)`：字段映射及自动填充
- `@TableLogic`：逻辑删除字段
- `@Version`：乐观锁字段

示例：如上 `User` 类，`createTime`/`updateTime` 受 `MetaObjectHandler` 自动填充。

---

## 4 主键策略（常用）

- `IdType.AUTO`：数据库自增（依赖 DB 自增）
- `IdType.ASSIGN_ID`：雪花算法（128 位字符串或 Long）
- `IdType.ASSIGN_UUID`：UUID
- `IdType.INPUT`：用户手动设置（常用于外部系统主键）

选择建议：分布式场景优先 `ASSIGN_ID`，单库可选 `AUTO`。

---

## 5 BaseMapper 与 IService

- `BaseMapper<T>`：直接在 Mapper 层提供通用方法（`insert`、`deleteById`、`selectById`、`selectList` 等）。
- `IService<T>`：在业务层包装通用方法（`save`、`saveBatch`、`page`、`list` 等），配合 `ServiceImpl` 使用。

推荐：Controller 调用 Service，Service 调用 Mapper；只在非常简单场景使用 ActiveRecord。

---

## 6 条件构造器（实用示例）

优先用 `LambdaQueryWrapper` 避免硬编码字段名。

```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>()
      .like(StringUtils.isNotBlank(name), User::getName, name)
      .eq(age != null, User::getAge, age)
      .between(start != null && end != null, User::getCreateTime, start, end)
      .orderByDesc(User::getUpdateTime);

IPage<User> page = userService.page(new Page<>(pageNo, pageSize), wrapper);
```

常用方法：`eq`、`ne`、`gt`、`ge`、`lt`、`le`、`like`、`in`、`between`、`orderByAsc/Desc`、`nested`。

注意：布尔条件参数第一位可传 `boolean`，用于按需拼接。

---

## 7 分页配置与使用

在 `MybatisPlusInterceptor` 中添加 `PaginationInnerInterceptor`：

```java
interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
```

使用：

```java
IPage<User> page = userService.page(new Page<>(1, 10), wrapper);
List<User> list = page.getRecords();
long total = page.getTotal();
```

---

## 8 自动填充（MetaObjectHandler）

实现示例：

```java
public class MyMetaObjectHandler implements MetaObjectHandler {
      @Override
      public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
      }

      @Override
      public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
      }
}
```

注册为 Spring Bean 即可自动生效。

---

## 9 逻辑删除

使用 `@TableLogic` 标记字段，常见约定：未删除值 `0`，已删除值 `1`。可通过 `GlobalConfig.DbConfig` 统一设置默认值。

示例：

```java
@TableLogic
private Integer deleted; // 0 = 未删除, 1 = 已删除
```

注意：`deleteById` 实际会被逻辑删除拦截器处理为更新操作。

---

## 10 乐观锁

使用 `@Version` 注解字段，并在 `MybatisPlusInterceptor` 中添加 `OptimisticLockerInnerInterceptor`。更新时，MP 会在 SQL 中自动带上版本号条件并在成功后自增版本。

示例：

```java
@Version
private Integer version;

// update 语句会变为：WHERE id = ? AND version = ?
```

并发冲突时，更新返回 0，可以按需重试或提示用户。

---

## 11 全局配置要点

- `GlobalConfig.DbConfig`：设置 `idType`、`tablePrefix`、`logicDeleteField`、`logicDeleteValue`、`logicNotDeleteValue` 等。
- `map-underscore-to-camel-case`：建议开启驼峰映射。

示例设置（伪代码，按项目调整）：

```java
GlobalConfig globalConfig = new GlobalConfig();
GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
dbConfig.setLogicDeleteField("deleted");
dbConfig.setLogicDeleteValue("1");
dbConfig.setLogicNotDeleteValue("0");
globalConfig.setDbConfig(dbConfig);
```

---

## 12 常用注解补充

- `@EnumValue`：枚举持久化值
- `@KeySequence`：Oracle 等需要序列的场景
- `@OrderBy`：实体层排序注解（非所有版本通用）
- `@InterceptorIgnore`：忽略某些 MP 拦截器（例如分页）

---

## 13 插件体系与顺序

`MybatisPlusInterceptor` 是统一入口。常见顺序（影响 SQL 解析结果，按需微调）：

1. 多租户/动态表名
2. 分页/乐观锁
3. 非法 SQL 拦截/防全表更新删除（BlockAttackInnerInterceptor）

---

## 14 FastAutoGenerator（代码生成）示例

最小示例：

```java
FastAutoGenerator.create("jdbc:mysql://localhost:3306/demo", "root", "root")
      .globalConfig(builder -> builder.author("you").outputDir(System.getProperty("user.dir") + "/src/main/java"))
      .packageConfig(builder -> builder.parent("com.example"))
      .strategyConfig(builder -> builder.addInclude("user").entityBuilder().enableLombok())
      .execute();
```

注意：生成后请审查模板，尤其是 DTO、注释和安全相关代码。

---

## 15 使用习惯与建议

- 单表 CRUD 与简单条件查询优先用 MP，代码短且可维护。
- 复杂联表、统计或性能敏感的 SQL 使用自定义 XML 或手写 SQL 优化。
- 优先使用 `LambdaWrapper` 系列避免手写字段名。
- 在关键更新处使用乐观锁并合理处理更新失败情况。
- 开发阶段打开 `showSql`，生产环境关闭并使用合理的监控链路。

---

## 16 实战练习（建议步骤）

1. 使用 FastAutoGenerator 生成 `user` 模块代码。
2. 实现 `UserController` 的分页查询和条件搜索接口（用 `LambdaQueryWrapper`）。
3. 实现创建、更新（包含乐观锁）、删除（逻辑删除）接口。
4. 为关键接口编写单元测试，验证自动填充、逻辑删除、乐观锁行为。
