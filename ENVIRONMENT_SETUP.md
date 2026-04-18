# 环境变量配置指南

## 📋 概述

本项目使用 `.env` 文件管理敏感配置信息（如 JWT 密钥、数据库密码等），避免将敏感信息硬编码在代码或版本控制中。

## 🚀 快速开始

### 1. 创建 .env 文件

复制模板文件并修改：
```powershell
copy .env.example .env
```

### 2. 生成强随机密钥

**方式一：使用提供的 PowerShell 脚本（推荐）**
```powershell
.\generate-jwt-secret.ps1
```
脚本会自动生成强随机密钥，并可选择自动更新 `.env` 文件。

**方式二：手动生成（PowerShell）**
```powershell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
```

**方式三：使用 Git Bash/WSL**
```bash
openssl rand -base64 64
```

### 3. 配置 .env 文件

编辑 `.env` 文件，填入生成的密钥：
```env
JWT_SECRET=your-generated-secret-key-here
```

### 4. 重启应用

Spring Boot 会自动加载 `.env` 文件中的环境变量。

## 📁 文件说明

| 文件 | 说明 | 是否提交到 Git |
|------|------|---------------|
| `.env` | 实际的环境变量配置文件 | ❌ 否（已加入 .gitignore） |
| `.env.example` | 环境变量模板文件 | ✅ 是 |
| `generate-jwt-secret.ps1` | JWT 密钥生成脚本 | ✅ 是 |

## 🔒 安全注意事项

### ⚠️ 重要规则

1. **永远不要**将 `.env` 文件提交到版本控制系统
2. **永远不要**在代码中硬编码敏感信息
3. 生产环境应使用更强的密钥（至少 64 字符）
4. 定期轮换密钥

### 不同环境的配置策略

#### 开发环境 (dev)
- 可以使用 `.env` 文件
- 密钥可以相对简单（但仍建议至少 32 字符）
- Token 过期时间可设置较长（如 2 小时）

#### 测试环境 (test)
- 可以使用 `.env` 文件
- 密钥强度中等
- Token 过期时间适中（如 1.5 小时）

#### 生产环境 (prod)
- **禁止**使用 `.env` 文件
- 必须通过以下方式之一注入环境变量：
  - Docker: `docker run -e JWT_SECRET=...`
  - Kubernetes: 使用 Secret 资源
  - Linux: `export JWT_SECRET=...`
  - Windows: `setx JWT_SECRET "..." /M`
- 密钥必须是强随机字符串（至少 64 字符）
- Token 过期时间应较短（如 1 小时）

## 🐳 Docker 部署示例

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
COPY target/SpringbootDemo.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# 运行时注入环境变量
docker run -d \
  -e JWT_SECRET="your-super-strong-production-secret-key" \
  -p 8080:8080 \
  springboot-demo
```

或使用 Docker Compose：
```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=${JWT_SECRET}
    env_file:
      - .env.prod  # 生产环境的 .env 文件（不提交到 Git）
```

## ☸️ Kubernetes 部署示例

```yaml
# k8s-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
type: Opaque
data:
  JWT_SECRET: <base64-encoded-secret>
```

```yaml
# k8s-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-demo
spec:
  template:
    spec:
      containers:
        - name: app
          image: springboot-demo:latest
          env:
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: jwt-secret
                  key: JWT_SECRET
```

## 🔧 故障排查

### 问题：应用启动时提示密钥无效

**解决方案：**
1. 检查 `.env` 文件是否存在于项目根目录
2. 确认 `JWT_SECRET` 变量已正确设置
3. 确认密钥长度至少 32 字符
4. 重启应用

### 问题：环境变量未生效

**解决方案：**
1. 确认已添加 `spring-dotenv` 依赖
2. 检查 `.env` 文件格式是否正确（无多余空格）
3. 查看应用日志确认环境变量已加载

### 问题：IDE 中显示警告

**解决方案：**
这是正常的，因为 `.env` 文件不在版本控制中。其他开发者需要自行创建 `.env` 文件。

## 📝 最佳实践

1. ✅ 为每个环境维护独立的配置
2. ✅ 使用强随机密钥
3. ✅ 定期轮换密钥
4. ✅ 在 CI/CD 管道中使用 secrets 管理
5. ❌ 不要在日志中输出密钥
6. ❌ 不要通过邮件/聊天工具传输密钥
7. ❌ 不要将 `.env` 文件发送给他人

## 🔗 相关文档

- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [JWT 最佳实践](https://jwt.io/introduction)
- [OWASP 密钥管理指南](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
