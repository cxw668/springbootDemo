# 05 - SpringDoc OpenAPI 接口文档集成

> 本文档记录如何使用 SpringDoc OpenAPI（Swagger UI）自动生成 REST API 在线文档，替代传统的 Swagger 2。

## 一、技术选型说明

### 1.1 为什么选择 SpringDoc？

| 特性 | Swagger 2 (springfox) | SpringDoc OpenAPI |
|---|---|---|
| **Spring Boot 3 支持** | ❌ 不支持 | ✅ 原生支持 |
| **Jakarta EE 支持** | ❌ 仅 javax | ✅ 使用 jakarta |
| **OpenAPI 规范版本** | OpenAPI 2.0 (Swagger) | OpenAPI 3.0+ |
| **维护状态** | 已停止更新 | 活跃维护 |
| **UI 现代化程度** | 较旧 | 现代化界面 |

**结论：** Spring Boot 3.x 项目必须使用 SpringDoc OpenAPI。

---

## 二、系统架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Browser)                          │
│                    访问 http://localhost:8080/swagger-ui.html     │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP GET /swagger-ui.html
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SpringDoc Auto-Configuration                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  SpringDocWebMvcConfigurer                                 │  │
│  │  ├─ 注册 Swagger UI 资源映射                                │  │
│  │  ├─ 注册 OpenAPI JSON/YAML 端点                            │  │
│  │  └─ 扫描 @RestController + @Operation 注解                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    UserController (REST 层)                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  @Tag(name = "用户管理")                                   │  │
│  │  @Operation(summary = "分页查询用户")                      │  │
│  │  @Parameter(description = "页码")                          │  │
│  │  @ApiResponse(responseCode = "200", description = "成功")  │  │
│  └───────────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              OpenAPI Specification Generator                      │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ 反射扫描 Controller   │  │ 解析 Validation 注解             │ │
│  │ 提取方法签名          │  │ 生成 Schema 定义                 │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              生成的 OpenAPI 文档 (JSON/YAML)                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  /v3/api-docs           → OpenAPI JSON 规范                │  │
│  │  /v3/api-docs.yaml      → OpenAPI YAML 规范                │  │
│  │  /swagger-ui.html       → Swagger UI 可视化界面            │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**工作流程：**
1. SpringDoc 自动扫描所有 `@RestController` 类
2. 解析方法上的 `@Operation`、`@Parameter`、`@ApiResponse` 等注解
3. 结合实体类的 Validation 注解生成数据模型 Schema
4. 运行时动态生成符合 OpenAPI 3.0 规范的 JSON/YAML 文档
5. Swagger UI 读取文档并渲染成交互式页面

---

## 三、快速开始

### 3.1 添加依赖

在 `pom.xml` 中添加 SpringDoc 依赖：

```xml
<!-- SpringDoc OpenAPI (Spring Boot 3.x) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**注意：** 
- Spring Boot 3.x 使用 `springdoc-openapi-starter-webmvc-ui`
- Spring Boot 2.x 使用 `springdoc-openapi-ui`（版本 1.x）
- 版本号需与 Spring Boot 版本兼容（参考 [SpringDoc 官方兼容性表](https://springdoc.org/)）

### 3.2 基础配置

在 `application.yml` 中配置 SpringDoc：

```yaml
springdoc:
  api-docs:
    enabled: true                    # 启用 API 文档
    path: /v3/api-docs               # OpenAPI JSON 路径
  swagger-ui:
    enabled: true                    # 启用 Swagger UI
    path: /swagger-ui.html           # Swagger UI 访问路径
    tags-sorter: alpha               # 标签按字母排序
    operations-sorter: alpha         # 接口按字母排序
  default-flat-param-object: true    # 参数对象扁平化展示
```

### 3.3 验证安装

启动应用后访问：
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

如果看到交互式文档页面，说明集成成功！

---

## 四、核心注解使用

### 4.1 类级别注解

#### @Tag - API 分组标签

```java
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户的增删改查操作")
public class UserController {
    // ...
}
```

**效果：** 在 Swagger UI 中创建一个名为"用户管理"的分组卡片。

#### @Hidden - 隐藏整个 Controller

```java
@RestController
@Hidden  // 该 Controller 不会出现在文档中
public class InternalController {
    // ...
}
```

### 4.2 方法级别注解

#### @Operation - 接口描述

```java
@GetMapping("/page")
@Operation(
    summary = "分页查询用户",
    description = "支持按姓名、年龄、手机号、时间范围筛选，返回分页结果"
)
public Result<IPage<User>> pageQuery(
        @RequestParam(defaultValue = "1") int pageNo,
        @RequestParam(defaultValue = "10") int pageSize) {
    // ...
}
```

**常用属性：**
- `summary`: 接口简短描述（显示在列表）
- `description`: 详细说明（点击展开后显示）
- `tags`: 覆盖类级别的 @Tag
- `deprecated`: 标记接口已废弃

#### @Parameters & @Parameter - 参数说明

```java
@GetMapping("/page")
@Operation(summary = "分页查询用户")
public Result<IPage<User>> pageQuery(
        @Parameter(description = "页码，从1开始", example = "1")
        @RequestParam(defaultValue = "1") int pageNo,
        
        @Parameter(description = "每页大小，最大100", example = "10")
        @RequestParam(defaultValue = "10") int pageSize,
        
        @Parameter(description = "姓名（模糊查询）", required = false)
        @RequestParam(required = false) String name,
        
        @Parameter(description = "年龄", required = false)
        @RequestParam(required = false) Integer age,
        
        @Parameter(description = "手机号", required = false)
        @RequestParam(required = false) String phone,
        
        @Parameter(description = "开始时间", example = "2024-01-01T00:00:00")
        @RequestParam(required = false) LocalDateTime start,
        
        @Parameter(description = "结束时间", example = "2024-12-31T23:59:59")
        @RequestParam(required = false) LocalDateTime end) {
    // ...
}
```

**常用属性：**
- `description`: 参数说明
- `required`: 是否必填
- `example`: 示例值
- `deprecated`: 标记参数已废弃
- `hidden`: 隐藏参数

#### @RequestBody - 请求体说明

```java
@PostMapping
@Operation(summary = "创建用户")
public Result<Boolean> create(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "用户信息",
                required = true,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = User.class)
                )
        )
        @Validated(User.CreateGroup.class) 
        @RequestBody User user) {
    // ...
}
```

**简化写法（推荐）：** SpringDoc 会自动识别 `@RequestBody`，通常不需要额外注解。

### 4.3 响应级别注解

#### @ApiResponses & @ApiResponse - 响应说明

```java
@PostMapping
@Operation(summary = "创建用户")
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "创建成功",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Result.class)
                )
        ),
        @ApiResponse(
                responseCode = "422",
                description = "参数校验失败",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Result.class)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "服务器内部错误"
        )
})
public Result<Boolean> create(@Validated @RequestBody User user) {
    // ...
}
```

**常用响应码：**
- `200`: 成功
- `201`: 创建成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 禁止访问
- `404`: 资源不存在
- `422`: 参数校验失败
- `500`: 服务器错误

### 4.4 实体类注解

#### @Schema - 数据模型说明

```java
@Getter
@Setter
@ToString
@TableName("`user`")
@Schema(description = "用户实体")
public class User implements Serializable {

    @Schema(description = "用户ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId("id")
    private Long id;

    @Schema(description = "姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @TableField("name")
    @NotBlank(message = "姓名不能为空", groups = CreateGroup.class)
    private String name;

    @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
    @TableField("age")
    @NotNull(message = "年龄不能为空", groups = CreateGroup.class)
    private Integer age;

    @Schema(description = "手机号", example = "13800138000", pattern = "^1[3-9]\\d{9}$")
    @TableField("phone")
    @Phone(message = "手机号格式不正确", groups = CreateGroup.class)
    private String phone;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除标识：0-未删除，1-已删除", hidden = true)
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Byte deleted;

    @Schema(description = "乐观锁版本号", accessMode = Schema.AccessMode.READ_ONLY)
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;
}
```

**常用属性：**
- `description`: 字段说明
- `example`: 示例值
- `requiredMode`: 是否必填
- `minimum`/`maximum`: 数值范围
- `pattern`: 正则表达式
- `accessMode`: 访问模式（READ_ONLY/WRITE_ONLY/READ_WRITE）
- `hidden`: 隐藏字段
- `deprecated`: 标记已废弃

---

## 五、完整示例：UserController 文档化

### 5.1 改造后的 Controller

```java
package demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import demo.common.BizException;
import demo.common.Result;
import demo.model.User;
import demo.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "提供用户的增删改查及分页查询功能")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    @Operation(
            summary = "分页查询用户",
            description = "支持按姓名、年龄、手机号、时间范围进行筛选，返回分页结果"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public Result<IPage<User>> pageQuery(
            @Parameter(description = "页码，从1开始", example = "1")
            @Min(value = 1, message = "页码不能小于1") 
            @RequestParam(defaultValue = "1") int pageNo,
            
            @Parameter(description = "每页大小，范围1-100", example = "10")
            @Min(value = 1, message = "页大小不能小于1") 
            @Max(value = 100, message = "页大小不能大于100") 
            @RequestParam(defaultValue = "10") int pageSize,
            
            @Parameter(description = "姓名（模糊查询）", required = false)
            @RequestParam(required = false) String name,
            
            @Parameter(description = "年龄", required = false)
            @RequestParam(required = false) Integer age,
            
            @Parameter(description = "手机号", required = false)
            @RequestParam(required = false) String phone,
            
            @Parameter(description = "开始时间", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) LocalDateTime start,
            
            @Parameter(description = "结束时间", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(userService.pageQuery(pageNo, pageSize, name, age, phone, start, end));
    }

    /**
     * 条件搜索用户
     */
    @GetMapping("/search")
    @Operation(
            summary = "条件搜索用户",
            description = "不分页，返回所有符合条件的用户列表"
    )
    public Result<List<User>> search(
            @Parameter(description = "姓名（模糊查询）")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "年龄")
            @RequestParam(required = false) Integer age,
            
            @Parameter(description = "手机号")
            @RequestParam(required = false) String phone,
            
            @Parameter(description = "开始时间")
            @RequestParam(required = false) LocalDateTime start,
            
            @Parameter(description = "结束时间")
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(userService.pageQuery(1, Integer.MAX_VALUE, name, age, phone, start, end).getRecords());
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(
            summary = "创建用户",
            description = "创建新用户，需提供姓名、年龄、手机号"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "422", description = "参数校验失败"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public Result<Boolean> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户信息",
                    required = true
            )
            @Validated(User.CreateGroup.class) 
            @RequestBody User user) {
        boolean saved = userService.save(user);
        if (!saved) {
            throw new BizException("用户创建失败");
        }
        return Result.success(true);
    }

    /**
     * 更新用户
     */
    @PutMapping
    @Operation(
            summary = "更新用户",
            description = "更新用户信息，需提供ID和版本号（乐观锁）"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "422", description = "参数校验失败或版本冲突"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public Result<Boolean> update(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户信息（需包含id和version）",
                    required = true
            )
            @Validated(User.UpdateGroup.class) 
            @RequestBody User user) {
        boolean updated = userService.updateById(user);
        if (!updated) {
            throw new BizException("用户更新失败，可能版本号过期或记录不存在");
        }
        return Result.success(true);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "删除用户",
            description = "逻辑删除用户，不会真正从数据库删除"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Result<Boolean> delete(
            @Parameter(description = "用户ID", example = "1", required = true)
            @PathVariable Long id) {
        boolean removed = userService.removeById(id);
        if (!removed) {
            throw new BizException("用户不存在");
        }
        return Result.success(true);
    }
}
```

---

## 六、高级配置

### 6.1 自定义 OpenAPI 信息

创建配置类设置 API 文档元信息：

```java
package demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpringBootDemo API 文档")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3 + MyBatis-Plus 的用户管理系统 API")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com")
                                .url("https://github.com/example"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
```

**效果：** 在 Swagger UI 顶部显示 API 标题、版本、描述、联系方式等信息。

### 6.2 多环境配置

#### 生产环境禁用文档

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

#### 测试/开发环境启用

```yaml
# application-dev.yml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

### 6.3 分组配置（多模块项目）

如果项目有多个模块，可以按模块分组：

```java
@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户管理")
                .pathsToMatch("/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("订单管理")
                .pathsToMatch("/order/**")
                .build();
    }
}
```

**效果：** Swagger UI 左上角会出现下拉框，可选择不同的 API 分组。

### 6.4 安全认证配置

如果项目使用了 JWT 或 OAuth2，可以配置全局安全方案：

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .components(new Components()
                    .addSecuritySchemes("bearerAuth",
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .info(new Info()
                    .title("API 文档")
                    .version("1.0.0"));
}
```

**效果：** Swagger UI 右上角会出现 "Authorize" 按钮，可输入 JWT Token。

---

## 七、常见问题与解决方案

### 7.1 问题：Swagger UI 页面空白

**原因：** 静态资源映射冲突或依赖版本不兼容。

**解决方案：**
1. 检查 Spring Boot 和 SpringDoc 版本兼容性
2. 确认没有自定义 `WebMvcConfigurer` 拦截 `/swagger-ui/**` 路径
3. 清除浏览器缓存或使用无痕模式访问

### 7.2 问题：实体类字段未显示

**原因：** Lombok 注解处理顺序问题或缺少 getter 方法。

**解决方案：**
1. 确保 Lombok 版本与 JDK 兼容
2. 检查 `@Getter`/`@Setter` 注解是否正确
3. 手动添加 getter/setter 方法测试

### 7.3 问题：Validation 注解未生效

**原因：** SpringDoc 默认不解析 Bean Validation 注解。

**解决方案：**
```yaml
springdoc:
  default-flat-param-object: true
```

或在实体类上使用 `@Schema` 显式标注。

### 7.4 问题：日期格式显示异常

**原因：** LocalDateTime 序列化格式问题。

**解决方案：**
```java
@Schema(description = "创建时间", example = "2024-01-01T12:00:00")
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

### 7.5 问题：接口太多导致页面加载慢

**解决方案：**
1. 启用懒加载：
```yaml
springdoc:
  swagger-ui:
    display-request-duration: true
    doc-expansion: none  # 默认折叠所有接口
```

2. 使用分组功能拆分模块
3. 生产环境禁用文档

---

## 八、最佳实践

### 8.1 注解使用原则

✅ **推荐做法：**
- 每个 Controller 类添加 `@Tag` 说明业务模块
- 每个接口方法添加 `@Operation` 说明功能
- 关键参数添加 `@Parameter` 说明用途和示例
- 重要接口添加 `@ApiResponses` 说明可能的响应
- 实体类关键字段添加 `@Schema` 说明

❌ **避免做法：**
- 不要为每个参数都添加注解（简单参数可省略）
- 不要重复描述（summary 和 description 要有区分）
- 不要在 production 环境暴露文档

### 8.2 文档维护策略

1. **代码即文档：** 优先通过注解生成文档，减少手工维护
2. **及时更新：** 修改接口时同步更新注解
3. **示例值真实：** `example` 使用真实可用的示例数据
4. **版本管理：** API 变更时更新 `Info.version`

### 8.3 性能优化

1. **按需启用：** 仅在 dev/test 环境启用文档
2. **缓存配置：** 生产环境可缓存 OpenAPI JSON
3. **精简注解：** 避免过度注解影响启动速度

---

## 九、实战练习

### 9.1 任务清单

- [X] 添加 SpringDoc 依赖到 `pom.xml`
- [X] 配置 `application.yml` 中的 springdoc 参数
- [X] 创建 `OpenApiConfig` 配置类设置 API 元信息
- [X] 为 `UserController` 添加 `@Tag` 和 `@Operation` 注解
- [X] 为所有接口参数添加 `@Parameter` 说明
- [X] 为 `User` 实体类添加 `@Schema` 注解
- [X] 启动应用并访问 http://localhost:8080/swagger-ui.html
- [X] 测试"Try it out"功能，验证接口调用
- [X] 配置生产环境禁用文档

### 9.2 验证步骤

1. 启动应用
2. 浏览器访问 http://localhost:8080/swagger-ui.html
3. 查看"用户管理"分组下的所有接口
4. 点击任意接口展开，查看参数说明和示例
5. 点击"Try it out"，填写参数并执行
6. 查看响应结果是否符合预期
7. 点击"Download"下载 OpenAPI JSON/YAML 文件

---

## 十、参考资料

- **SpringDoc 官方文档:** https://springdoc.org/
- **OpenAPI 规范:** https://swagger.io/specification/
- **Swagger UI GitHub:** https://github.com/swagger-api/swagger-ui
- **SpringDoc 示例项目:** https://github.com/springdoc/springdoc-openapi-demos

---

## 附录：常用注解速查表

| 注解 | 作用位置 | 用途 |
|---|---|---|
| `@Tag` | Class | API 分组标签 |
| `@Operation` | Method | 接口描述 |
| `@Parameter` | Parameter | 参数说明 |
| `@RequestBody` | Parameter | 请求体说明 |
| `@ApiResponse` | Method | 响应说明 |
| `@ApiResponses` | Method | 多个响应组合 |
| `@Schema` | Class/Field | 数据模型说明 |
| `@Hidden` | Class/Method | 隐藏接口 |
| `@SecurityRequirement` | Class/Method | 安全要求 |

**导入包路径：**
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Hidden;
```
