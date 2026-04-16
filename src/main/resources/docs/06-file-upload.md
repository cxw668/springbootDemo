# 3.5 文件上传 - 学习与实践

## 📚 学习目标

1. 掌握 Spring Boot 文件上传基本原理
2. 熟练使用 `MultipartFile` 处理文件
3. 实现文件类型、大小校验
4. 实现本地文件存储
5. （进阶）集成阿里云 OSS 云存储

---

## 🎯 核心知识点

### 1. MultipartFile 简介

`MultipartFile` 是 Spring MVC 提供的文件上传接口，封装了上传文件的各项信息：

```java
public interface MultipartFile {
    String getName();           // 表单字段名
    String getOriginalFilename(); // 原始文件名
    String getContentType();    // 文件 MIME 类型
    long getSize();             // 文件大小（字节）
    byte[] getBytes();          // 文件内容
    InputStream getInputStream(); // 输入流
    void transferTo(File dest); // 保存到目标文件
}
```

### 2. 文件上传配置

在 `application.yaml` 中配置文件上传限制：

```yaml
spring:
  servlet:
    multipart:
      enabled: true              # 启用文件上传
      max-file-size: 10MB        # 单个文件最大大小
      max-request-size: 50MB     # 整个请求最大大小
      file-size-threshold: 2KB   # 超过此大小写入磁盘
```

### 3. 文件存储策略

| 存储方式 | 优点 | 缺点 | 适用场景 |
|---------|------|------|---------|
| 本地存储 | 简单、快速 | 单点故障、扩展性差 | 小型项目、开发环境 |
| 云存储（OSS） | 高可用、CDN 加速 | 需要付费、配置复杂 | 生产环境、大型项目 |
| 分布式文件系统 | 可扩展、高可用 | 架构复杂 | 超大规模应用 |

---

## 🔧 实践步骤

### 步骤 1：数据库准备

为用户表添加头像字段：

```sql
-- 在 user 表中添加 avatar 字段
ALTER TABLE user ADD COLUMN avatar VARCHAR(500) COMMENT '用户头像URL';
```

更新 User 实体类：

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String email;
    private String phone;
    private String avatar; // 新增：头像URL
    private Integer age;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### 步骤 2：配置文件上传参数

在 `AppProperties` 中添加文件上传配置：

```java
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    // ... 其他配置
    
    private FileUpload fileUpload = new FileUpload();
    
    @Data
    public static class FileUpload {
        private String uploadPath = "./uploads";  // 上传目录
        private Long maxSize = 10 * 1024 * 1024L; // 10MB
        private List<String> allowedTypes = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
        );
        private List<String> allowedExtensions = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
        );
    }
}
```

在 `application.yaml` 中配置：

```yaml
app:
  file-upload:
    upload-path: ./uploads/avatars
    max-size: 10485760  # 10MB
    allowed-types:
      - image/jpeg
      - image/png
      - image/gif
      - image/webp
    allowed-extensions:
      - .jpg
      - .jpeg
      - .png
      - .gif
      - .webp
```

### 步骤 3：创建文件上传服务

创建 `FileUploadService` 接口和实现：

```java
package demo.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileUploadService {
    
    /**
     * 上传文件
     * @param file 上传的文件
     * @param subDir 子目录（如：avatars）
     * @return 文件访问路径
     */
    String uploadFile(MultipartFile file, String subDir);
    
    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void deleteFile(String filePath);
}
```

```java
package demo.service.impl;

import demo.common.AppProperties;
import demo.service.IFileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements IFileUploadService {

    private final AppProperties appProperties;

    @Override
    public String uploadFile(MultipartFile file, String subDir) {
        // 1. 校验文件
        validateFile(file);
        
        // 2. 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFileName = generateFileName(extension);
        
        // 3. 构建存储路径（按日期分目录）
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uploadDir = appProperties.getFileUpload().getUploadPath();
        String relativePath = StringUtils.isNotBlank(subDir) 
            ? subDir + "/" + dateDir 
            : dateDir;
        String fullPath = uploadDir + "/" + relativePath;
        
        // 4. 创建目录
        File dir = new File(fullPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 5. 保存文件
        String filePath = fullPath + "/" + newFileName;
        try {
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());
            log.info("文件上传成功: {}", filePath);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
        
        // 6. 返回相对路径（用于数据库存储）
        return "/" + relativePath + "/" + newFileName;
    }

    @Override
    public void deleteFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return;
        }
        
        String uploadDir = appProperties.getFileUpload().getUploadPath();
        String fullPath = uploadDir + filePath;
        
        File file = new File(fullPath);
        if (file.exists() && file.delete()) {
            log.info("文件删除成功: {}", fullPath);
        } else {
            log.warn("文件删除失败或不存在: {}", fullPath);
        }
    }

    /**
     * 校验文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 校验文件大小
        long maxSize = appProperties.getFileUpload().getMaxSize();
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                "文件大小不能超过 " + (maxSize / 1024 / 1024) + "MB"
            );
        }
        
        // 校验文件类型
        String contentType = file.getContentType();
        if (!appProperties.getFileUpload().getAllowedTypes().contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }
        
        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!appProperties.getFileUpload().getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
```

### 步骤 4：创建文件上传 Controller

```java
package demo.controller;

import demo.common.Result;
import demo.service.IFileUploadService;
import demo.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "文件管理", description = "文件上传、下载相关接口")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final IFileUploadService fileUploadService;
    private final IUserService userService;

    @Operation(summary = "上传用户头像", description = "支持 jpg、png、gif、webp 格式，最大 10MB")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId) {
        
        // 1. 上传文件
        String avatarPath = fileUploadService.uploadFile(file, "avatars");
        
        // 2. 更新用户头像
        userService.updateAvatar(userId, avatarPath);
        
        // 3. 返回结果
        Map<String, String> result = new HashMap<>();
        result.put("avatarUrl", avatarPath);
        result.put("message", "头像上传成功");
        
        return Result.success(result);
    }

    @Operation(summary = "通用文件上传", description = "上传文件到指定目录")
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "子目录（可选）") @RequestParam(value = "subDir", required = false) String subDir) {
        
        String filePath = fileUploadService.uploadFile(file, subDir);
        
        Map<String, String> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("message", "文件上传成功");
        
        return Result.success(result);
    }

    @Operation(summary = "删除文件", description = "根据文件路径删除文件")
    @DeleteMapping("/delete")
    public Result<Void> deleteFile(
            @Parameter(description = "文件路径") @RequestParam("filePath") String filePath) {
        
        fileUploadService.deleteFile(filePath);
        return Result.success(null, "文件删除成功");
    }
}
```

### 步骤 5：更新 UserService

在 `IUserService` 中添加更新头像方法：

```java
/**
 * 更新用户头像
 */
void updateAvatar(Long userId, String avatarPath);
```

在 `UserServiceImpl` 中实现：

```java
@Override
public void updateAvatar(Long userId, String avatarPath) {
    User user = getById(userId);
    if (user == null) {
        throw new BizException(BizCode.USER_NOT_FOUND);
    }
    
    // 删除旧头像
    if (StringUtils.isNotBlank(user.getAvatar())) {
        fileUploadService.deleteFile(user.getAvatar());
    }
    
    // 更新新头像
    user.setAvatar(avatarPath);
    updateById(user);
}
```

注意：需要在 `UserServiceImpl` 中注入 `IFileUploadService`。

### 步骤 6：配置静态资源访问

为了让上传的文件可以被访问，需要配置静态资源映射：

创建 `WebMvcConfig.java`（如果已存在则添加方法）：

```java
package demo.config;

import demo.common.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传目录到 /uploads/** 路径
        String uploadPath = appProperties.getFileUpload().getUploadPath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

---

## 🧪 测试验证

### 1. 使用 HTTP Client 测试

创建 `file-upload.http` 测试文件：

```http
### 上传用户头像
POST http://localhost:8080/api/files/avatar
Content-Type: multipart/form-data; boundary=boundary123

--boundary123
Content-Disposition: form-data; name="file"; filename="test-avatar.jpg"
Content-Type: image/jpeg

< ./test-avatar.jpg
--boundary123
Content-Disposition: form-data; name="userId"

--boundary123--

### 通用文件上传
POST http://localhost:8080/api/files/upload?subDir=documents
Content-Type: multipart/form-data; boundary=boundary456

--boundary456
Content-Disposition: form-data; name="file"; filename="document.pdf"
Content-Type: application/pdf

< ./document.pdf
--boundary456--

### 删除文件
DELETE http://localhost:8080/api/files/delete?filePath=/avatars/2024/01/15/abc123.jpg
```

### 2. 使用 Swagger UI 测试

访问 `http://localhost:8080/swagger-ui.html`，找到"文件管理"模块进行在线测试。

### 3. 验证文件存储

检查项目根目录下的 `uploads/avatars` 文件夹，确认文件已正确保存。

---

## ⚠️ 常见问题

### 1. 文件大小超限

**错误信息：** `Maximum upload size exceeded`

**解决方案：** 检查 `application.yaml` 中的配置：
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB
```

### 2. 文件类型不被允许

**错误信息：** `不支持的文件类型: application/octet-stream`

**解决方案：** 
- 前端上传时设置正确的 `Content-Type`
- 或在后端放宽类型限制

### 3. 中文文件名乱码

**解决方案：** 使用 UUID 生成文件名，避免使用原始文件名。

### 4. 上传目录权限问题

**错误信息：** `Access denied`

**解决方案：** 确保应用有写入权限：
```bash
# Linux/Mac
chmod -R 755 uploads/

# Windows
# 右键文件夹 → 属性 → 安全 → 编辑权限
```

---

## 🚀 进阶：集成阿里云 OSS

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```

### 2. 配置 OSS 参数

```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
    base-path: springboot-demo
```

### 3. 创建 OSS 服务

```java
@Service
@RequiredArgsConstructor
public class OssFileUploadService implements IFileUploadService {
    
    private final OSS ossClient;
    private final AliyunOssProperties ossProperties;
    
    @Override
    public String uploadFile(MultipartFile file, String subDir) {
        // 生成对象键名
        String objectKey = generateObjectKey(file.getOriginalFilename(), subDir);
        
        // 上传到 OSS
        try {
            ossClient.putObject(
                ossProperties.getBucketName(),
                objectKey,
                file.getInputStream()
            );
            
            // 返回访问 URL
            return "https://" + ossProperties.getBucketName() 
                 + "." + ossProperties.getEndpoint() 
                 + "/" + objectKey;
        } catch (Exception e) {
            throw new RuntimeException("OSS 上传失败", e);
        }
    }
    
    @Override
    public void deleteFile(String filePath) {
        // 从 URL 中提取 objectKey
        String objectKey = extractObjectKey(filePath);
        ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
    }
}
```

---

## 📝 学习总结

### 核心要点
1. ✅ 使用 `MultipartFile` 接收上传文件
2. ✅ 严格校验文件类型、大小、扩展名
3. ✅ 使用 UUID 生成唯一文件名，避免冲突
4. ✅ 按日期分目录存储，便于管理
5. ✅ 配置静态资源映射，使文件可访问
6. ✅ 及时清理旧文件，避免磁盘浪费

### 最佳实践
- 📌 永远不要信任用户上传的文件名
- 📌 始终校验文件类型和内容
- 📌 限制文件大小，防止 DoS 攻击
- 📌 生产环境使用云存储（OSS/COS）
- 📌 记录文件上传日志，便于追踪
- 📌 考虑文件去重（MD5 校验）

### 安全注意事项
- ⚠️ 防止上传恶意文件（exe、sh、php 等）
- ⚠️ 限制上传目录的执行权限
- ⚠️ 对图片进行二次处理（缩放、压缩）
- ⚠️ 使用 CDN 加速访问，减轻服务器压力

---

## 🎯 下一步

完成文件上传后，建议继续学习：
1. **3.4 定时任务** - 实现定期清理过期文件
2. **3.2 缓存 Redis** - 缓存文件元信息
3. **3.1 安全框架** - 为文件上传接口添加权限控制

---

## 📚 参考资料

- [Spring Boot 文件上传官方文档](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.multipart)
- [MultipartFile API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/multipart/MultipartFile.html)
- [阿里云 OSS 文档](https://help.aliyun.com/product/31815.html)
