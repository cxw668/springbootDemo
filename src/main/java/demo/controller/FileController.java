package demo.controller;

import demo.common.Result;
import demo.messaging.DomainEventPublisher;
import demo.service.IFileUploadService;
import demo.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "文件管理", description = "文件上传、下载相关接口")
@RestController
@RequestMapping("/api/files")
public class FileController {
    private final IFileUploadService fileUploadService;
    private final IUserService userService;
    private final DomainEventPublisher domainEventPublisher;

    public FileController(@Qualifier("fileUploadServiceImpl") IFileUploadService fileUploadService,
                          @Qualifier("userServiceImpl") IUserService userService,
                          DomainEventPublisher domainEventPublisher) {
        this.fileUploadService = fileUploadService;
        this.userService = userService;
        this.domainEventPublisher = domainEventPublisher;
    }
    @Operation(summary = "上传用户头像", description = "支持 jpg、png、gif、webp 格式，最大 10MB")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId) {

        // 1. 上传文件
        String avatarPath = fileUploadService.uploadFile(file, "avatars");

        // 2. 更新用户头像
        userService.updateAvatar(userId, avatarPath);
        domainEventPublisher.publishFileUploaded(userId, "avatars", avatarPath, file);

        // 3. 返回结果
        Map<String, String> result = new HashMap<>();
        result.put("avatarUrl", avatarPath);
        result.put("message", "头像上传成功");

        return Result.success(result);
    }

    @Operation(summary = "通用文件上传", description = "上传文件到指定目录，需要 user:upload 权限")
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('user:upload')")
    public Result<Map<String, String>> uploadFile(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "子目录（可选）") @RequestParam(value = "subDir", required = false) String subDir) {

        String filePath = fileUploadService.uploadFile(file, subDir);
        domainEventPublisher.publishFileUploaded(null, subDir, filePath, file);

        Map<String, String> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("message", "文件上传成功");

        return Result.success(result);
    }

    @Operation(summary = "删除文件", description = "根据文件路径删除文件")
    @DeleteMapping("/delete")
    public Result<String> deleteFile(
            @Parameter(description = "文件路径") @RequestParam("filePath") String filePath) {

        fileUploadService.deleteFile(filePath);
        return Result.success("文件删除成功");
    }
}
