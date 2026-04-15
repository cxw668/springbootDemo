# API 接口测试指南

本项目使用 **HTTP Client 文件**（`.http`）进行接口测试，这是 IntelliJ IDEA 原生支持 / VS Code（REST Client 插件）支持的高效 API 测试方式。

---

## 为什么选择 `.http` 文件？

| 优势 | 说明 |
|------|------|
| **零配置** | IDEA 原生支持，VS Code 安装 REST Client 插件即可 |
| **版本可控** | 文件随代码提交 Git，团队共享 |
| **环境变量管理** | 通过 `@` 变量实现多环境切换 |
| **支持脚本** | 可解析 JSON 响应、链式传参、自动化断言 |

---

## 快速开始

### 1. 安装插件（VS Code）

搜索安装 **REST Client** 插件，然后在 `.http` 文件中点击请求上方的 **"Send Request"** 即可执行。

### 2. 定义环境变量

```http
@baseUrl = http://127.0.0.1:8080
@token = your-jwt-token-here
```

### 3. 执行请求

在每个 `###` 分隔的请求块上方点击 **Send Request**，或使用快捷键：
- **IDEA**: 点击请求旁的绿色播放按钮，或 `Ctrl+Alt+Enter`
- **VS Code**: `Alt+Enter`（Windows）/ `Option+Enter`（Mac）

---

## 核心测试场景

### CRUD 基础操作

```http
### 创建用户
POST {{baseUrl}}/user
Content-Type: application/json

{
  "name": "测试用户",
  "age": 25
}

### 分页查询
GET {{baseUrl}}/user/page?pageNo=1&pageSize=10
Authorization: Bearer {{token}}
```

### 请求链（捕获响应）

通过 `# @name` 注解捕获响应，后续请求中引用：

```http
### 创建用户并命名响应
# @name createUser
POST {{baseUrl}}/user
Content-Type: application/json

{
  "name": "ChainTest",
  "age": 30
}

### 使用上一步返回的 ID 查询
GET {{baseUrl}}/user/search?name=ChainTest
Authorization: Bearer {{token}}
```

### 多环境切换

在项目根目录创建 `.http-client.env.json` 文件：

```json
{
  "dev": {
    "baseUrl": "http://localhost:8080",
    "token": "dev-token-123"
  },
  "staging": {
    "baseUrl": "https://staging.example.com",
    "token": "staging-token-456"
  }
}
```

在 `.http` 文件中引用：

```http
GET {{baseUrl}}/user/page?pageNo=1&pageSize=10
Authorization: Bearer {{token}}
```

通过 IDE 右上角的下拉菜单切换环境。

---

## 项目测试文件结构

当前项目使用 `src/api-test.http` 作为主要测试文件，按以下模块组织：

| 模块 | 覆盖内容 |
|------|----------|
| **基础 CRUD** | 创建、查询、更新、删除 |
| **高级查询** | 分页、模糊搜索、时间范围 |
| **边界测试** | 空参数、越界页码、不存在的 ID |
| **并发测试** | 乐观锁冲突（version 验证） |
| **业务场景** | 创建后查询验证、批量操作 |

---

## 企业级最佳实践

### 1. 覆盖边界情况

| 场景 | 示例 |
|------|------|
| 空参数 | `GET /user/page?pageNo=0` |
| 越界参数 | `GET /user/page?pageNo=999999` |
| 非法类型 | `GET /user/page?pageNo=abc` |
| 乐观锁冲突 | 用旧 `version` 更新记录 |

### 2. 请求模板化

将重复的参数提取为变量：

```http
@pageSize = 10
@defaultName = test

GET {{baseUrl}}/user/page?pageNo=1&pageSize={{pageSize}}&name={{defaultName}}
```

### 3. 响应时间观察

每次请求后 IDE 会自动显示：
- **Response time**: 响应耗时
- **Status code**: HTTP 状态码

可用于初步性能评估，发现慢接口。

### 4. 自动化测试集成

可结合 `newman` 或 `curl` 将 `.http` 文件转为自动化测试：

```bash
# 使用 curl 执行测试
curl -X POST http://localhost:8080/user \
  -H "Content-Type: application/json" \
  -d '{"name":"auto-test","age":25}'
```

### 5. 注释与组织

- 每个请求使用 `###` 分隔并添加清晰标题
- 按功能分组（CRUD、高级、异常、并发）
- 添加行内注释说明预期行为

---

## 核心概念学习

### Content-Type 请求头

**什么时候需要添加 `Content-Type: application/json`？**

| 需要添加 | 不需要添加 |
|---------|-----------|
| `POST` / `PUT` / `PATCH` 带有请求体 | `GET` / `DELETE` 无请求体 |
| 使用 `@RequestBody` 接收参数 | 使用 `@RequestParam` 或 `@PathVariable` |
| 发送 JSON/XML 数据 | 参数通过 URL QueryString 传递 |

**为什么需要？**
```java
// 需要 Content-Type: application/json
@PostMapping
public boolean create(@RequestBody User user) { ... }

// 不需要 Content-Type（参数在 URL 中）
@GetMapping("/page")
public IPage<User> pageQuery(@RequestParam int pageNo) { ... }
```
Spring MVC 根据 `Content-Type` 选择对应的**消息转换器**（`HttpMessageConverter`）。如果不指定或错误指定，会导致：
- `415 Unsupported Media Type` — 服务器拒绝处理
- 参数解析失败 — 数据格式不匹配

### Authorization 请求头

**作用**：传递身份认证令牌（JWT Token）
```http
Authorization: Bearer {{token}}
```
- **Bearer** — 认证方案类型
- **{{token}}** — 实际的身份凭证
- 即使接口暂时不验证，养成携带 Token 的习惯有助于后续对接权限系统

### QueryString vs Request Body

| 方式 | 位置 | 示例 | 适用场景 |
|------|------|------|---------|
| QueryString | URL 后面 | `?name=Alice&age=18` | `GET` 请求，查询条件 |
| Request Body | 请求体内 | `{"name":"Alice","age":18}` | `POST/PUT` 请求，创建/更新数据 |
| PathVariable | URL 路径中 | `/user/123` | 获取单个资源 ID |

### 常见请求头速查表

| 请求头 | 说明 | 示例 |
|--------|------|------|
| `Content-Type` | 告诉服务器请求体格式 | `application/json` |
| `Authorization` | 身份认证令牌 | `Bearer token123` |
| `Accept` | 告诉客户端期望的响应格式 | `application/json` |
| `User-Agent` | 客户端标识 | `Mozilla/5.0` |

### Spring MVC 参数接收方式

| 注解 | 位置 | 示例 | 对应请求头 |
|------|------|------|-----------|
| `@RequestBody` | 请求体（JSON） | `{"name":"Alice"}` | `Content-Type: application/json` |
| `@RequestParam` | URL QueryString | `?name=Alice&age=18` | 不需要 |
| `@PathVariable` | URL 路径 | `/user/123` | 不需要 |
| `@RequestHeader` | 请求头 | `Authorization: Bearer xxx` | 直接读取 |

### 调试技巧

1. **查看请求详情** — IDE 会显示完整请求 URL、Headers、Body
2. **观察响应时间** — 每次请求后显示耗时，初步判断性能
3. **使用环境变量** — 通过 `@baseUrl` 等变量快速切换 dev/prod
4. **请求链测试** — 用 `# @name` 捕获响应，在后续请求中复用数据

---

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 请求返回 403 | 检查 `@token` 是否正确配置 |
| 响应乱码 | 确认服务器返回 `Content-Type: application/json; charset=UTF-8` |
| 变量不生效 | 检查变量名拼写，确认 `{{}}` 格式正确 |
| `.http` 文件不被识别 | VS Code 安装 REST Client 扩展 |