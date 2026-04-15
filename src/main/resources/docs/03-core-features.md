# 03 - 核心功能实现总结

> 本文档记录 Spring Boot + MyBatis-Plus 项目各核心功能的实现细节，用于梳理与巩固。

## 一、系统架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Browser/curl)                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP Request
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    UserController (REST 层)                       │
│  ┌───────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ POST      │  │ PUT      │  │ DELETE   │  │ GET           │  │
│  │ /create   │  │ /update  │  │ /{id}    │  │ /{id} /list   │  │
│  │ @Validated│  │ @Valid   │  │          │  │               │  │
│  │ CreateGrp │  │ UpdateGrp│  │          │  │               │  │
│  └─────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬───────┘  │
└────────┼─────────────┼────────────┼─────────────────┼──────────┘
         │             │            │                 │
         ▼             ▼            ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                   GlobalExceptionHandler                          │
│  ┌────────────────────────┐ ┌─────────────────────────────────┐ │
│  │ MethodArgumentNotValid │ │     BusinessException / Ex      │ │
│  │ → Result(400, msg)     │ │     → Result(code, msg)         │ │
│  └────────────────────────┘ └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    UserService / UserServiceImpl                   │
│         IService<User> → CRUD + 分页(pageQuery)                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    UserMapper (BaseMapper<User>)                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MyBatis-Plus Interceptors                      │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ PaginationInner      │  │ OptimisticLockerInner            │ │
│  │ (分页拦截器)          │  │ (乐观锁拦截器)                    │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ MyMetaObjectHandler  │  │ @TableLogic 逻辑删除拦截          │ │
│  │ (自动填充)            │  │ (SQL自动加WHERE deleted=0)       │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    H2 Database (`user` table)                     │
│  id | name | age | create_time | update_time | deleted | version │
└─────────────────────────────────────────────────────────────────┘
```

## 二、统一响应格式

### 2.1 Result\<T> 类
**路径:** `demo.common.Result`

```java
public record Result<T>(int code, String message, T data) {
    public static <T> Result<T> ok()       { return new Result<>(200, "操作成功", null); }
    public static <T> Result<T> ok(T data) { return new Result<>(200, "操作成功", data); }
    public static <T> Result<T> error(int code, String msg) { return new Result<>(code, msg, null); }
    public static <T> Result<T> error(BizCode code) { return new Result<>(code.getCode(), code.getMsg(), null); }
}
```

**设计要点：**
- 使用 Java 14+ `record` 语法，代码简洁且自带 getter/equals/hashCode/toString
- 泛型 `<T>` 适配任意返回数据类型
- 提供工厂方法简化调用

### 2.2 业务状态码枚举
**路径:** `demo.common.BizCode`

| 枚举 | 状态码 | 说明 |
|---|---|---|
| `USER_NOT_FOUND` | 404 | 用户不存在 |
| `USER_CREATE_FAILED` | 500 | 创建失败 |
| `USER_UPDATE_FAILED` | 500 | 更新失败 |

## 三、全局异常处理

**路径:** `demo.common.GlobalExceptionHandler`

### 3.1 处理的异常类型

| 异常 | HTTP状态码 | 返回信息 |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | 拼接所有字段校验错误信息 |
| `ConstraintViolationException` | 400 | 参数校验失败（非Body参数） |
| `BizException` | 自定义 | 业务异常（由BizCode决定） |
| `IllegalArgumentException` | 400 | 非法参数 |
| `Exception` | 500 | 系统内部错误 |

### 3.2 核心代码模式

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(400, msg);
    }
}
```

## 四、参数校验

### 4.1 Validation Groups 分组校验

| 分组接口 | 触发时机 | 校验注解 |
|---|---|---|
| `CreateGroup.class` | `POST /create` | `@NotBlank`、`@Size`、`@Min`、`@Max`、`@NotNull` |
| `UpdateGroup.class` | `PUT /update` | `@NotNull`(id) |

### 4.2 User 实体校验注解

```java
@TableId("id")
@NotNull(message = "ID不能为空", groups = UpdateGroup.class)
private Long id;

@TableField("name")
@NotBlank(message = "姓名不能为空", groups = CreateGroup.class)
@Size(min = 1, max = 50, message = "姓名长度必须在1-50之间", groups = CreateGroup.class)
private String name;

@TableField("age")
@NotNull(message = "年龄不能为空", groups = CreateGroup.class)
@Min(value = 0, message = "年龄不能小于0", groups = CreateGroup.class)
@Max(value = 150, message = "年龄不能超过150", groups = CreateGroup.class)
private Integer age;
```

### 4.3 Controller 使用方式

```java
@PostMapping("/create")
public Result<User> create(@Validated(CreateGroup.class) @RequestBody User user)

@PutMapping("/update")
public Result<User> update(@Validated(UpdateGroup.class) @RequestBody User user)
```

## 五、MyBatis-Plus 三大特性

### 5.1 自动填充（Auto-fill）

**实现类:** `demo.common.MyMetaObjectHandler`

```java
@Override
public void insertFill(MetaObject metaObject) {
    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    this.strictInsertFill(metaObject, "deleted", Byte.class, (byte) 0);      // 逻辑删除默认值
    this.strictInsertFill(metaObject, "version", Integer.class, 1);          // 乐观锁初始版本
}

@Override
public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
}
```

**实体字段配置：**

| 字段 | 填充策略 | 说明 |
|---|---|---|
| `createTime` | `FieldFill.INSERT` | 插入时自动填充 |
| `updateTime` | `FieldFill.INSERT_UPDATE` | 插入+更新都填充 |
| `deleted` | `FieldFill.INSERT` | 插入时默认0 |
| `version` | `FieldFill.INSERT` | 插入时默认1 |

### 5.2 逻辑删除（Logical Delete）

**配置：** `@TableLogic` 注解在 `deleted` 字段

| 操作 | 生成的 SQL |
|---|---|
| 查询 | `SELECT ... WHERE deleted = 0` (自动加条件) |
| 删除 | `UPDATE user SET deleted = 1 WHERE id = ?` (UPDATE代替DELETE) |
| 按ID查询 | `SELECT ... WHERE id = ? AND deleted = 0` |

### 5.3 乐观锁（Optimistic Lock）

**配置：** `@Version` 注解 + `OptimisticLockerInnerInterceptor`

**工作流程：**
1. 插入记录时 `version = 1`
2. 更新时 SQL 变为：`UPDATE user SET ..., version = version + 1 WHERE id = ? AND version = ?`
3. 如果 version 不匹配（记录已被其他事务修改），更新失败返回 0 条受影响

### 5.4 分页插件

**配置：** `PaginationInnerInterceptor(DbType.MYSQL)`

```java
// 分页查询示例
public IPage<User> pageQuery(int pageNum, int pageSize, 
        String sortField, String sortOrder, String name, Integer minAge) {
    Page<User> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(name)) wrapper.like(User::getName, name);
    if (minAge != null) wrapper.ge(User::getAge, minAge);
    if (StringUtils.hasText(sortField)) {
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.orderBy(true, isAsc, User.class, false, 
                new SFunction[]{(Function) User.class.getDeclaredMethod(sortField)});
    }
    return this.page(page, wrapper);
}
```

## 六、测试体系

### 6.1 测试结构

```
src/test/java/demo/
├── SpringbootDemoApplicationTests.java
│   ├── BasicCRUDTests          (5 tests)  - 增删改查
│   ├── PaginationTests         (5 tests)  - 分页、排序、过滤
│   ├── AutoFillTests           (2 tests)  - 自动填充
│   ├── LogicalDeleteTests      (2 tests)  - 逻辑删除
│   └── OptimisticLockTests     (2 tests)  - 乐观锁
└── UserControllerTests.java
    ├── createValidation        (3 tests)  - 参数校验
    └── updateValidation        (2 tests)  - 更新校验
```

### 6.2 测试关键技术

| 技术 | 用途 |
|---|---|
| `@SpringBootTest` | 启动完整 Spring 上下文 |
| `@ActiveProfiles("test")` | 加载测试配置（H2内存库） |
| `@Transactional` + `@Rollback` | 每个测试方法后自动回滚 |
| `MockMvc` | Controller层HTTP模拟测试 |
| `@Nested` | 按功能分组，测试报告更清晰 |

### 6.3 测试踩坑记录

| 问题 | 解决方案 |
|---|---|
| `user`是 H2 保留关键字 | `@TableName("\`user\`")` 用反引号转义 |
| `deleted`/`version` 插入后为 null | 实体字段加 `fill = FieldFill.INSERT` + Handler填充 |
| `updateTime`更新后时间相同 | 测试中加 `Thread.sleep(1000)` |
| `data.sql`空文件报错 | 删除data.sql，测试用 @Transactional 回滚 |
| Spring Boot 3.2 + H2 | 测试配置 `spring.sql.init.mode=never` |

## 七、项目文件清单

| 文件 | 包路径 | 说明 |
|---|---|---|
| `Result.java` | demo.common | 统一响应包装类 |
| `BizCode.java` | demo.common | 业务状态码枚举 |
| `BizException.java` | demo.common | 业务异常类 |
| `GlobalExceptionHandler.java` | demo.common | 全局异常处理器 |
| `MybatisPlusConfig.java` | demo.common | MP插件配置 |
| `MyMetaObjectHandler.java` | demo.common | 自动填充处理器 |
| `User.java` | demo.model | 实体类(含校验注解) |
| `UserController.java` | demo.controller | REST接口 |
| `IUserService.java` | demo.service | Service接口 |
| `UserServiceImpl.java` | demo.service.impl | Service实现 |
| `application-test.yml` | resources | 测试配置(H2) |
| `schema.sql` | test/resources | 测试库表结构 |
