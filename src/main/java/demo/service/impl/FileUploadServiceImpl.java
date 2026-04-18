
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
        String uploadDir = appProperties.getFileUpload().getUploadDir();
        String relativePath = StringUtils.isNotBlank(subDir)
                ? subDir + "/" + dateDir
                : dateDir;
        String fullPath = uploadDir + "/" + relativePath;

        // 4. 创建目录
        File dir = new File(fullPath);
        if (!dir.exists()) dir.mkdirs();

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
        return "/" + relativePath + "/" + newFileName;    }

    @Override
    public void deleteFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return;
        }

        String uploadDir = appProperties.getFileUpload().getUploadDir();
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
        long maxSize = appProperties.getFileUpload().getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "文件大小不能超过 " + (maxSize / 1024 / 1024) + "MB"
            );
        }

        // 第1层：校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        String[] allowedExtensions = appProperties.getFileUpload().getAllowedFileExtensions();
        boolean isExtensionAllowed = java.util.Arrays.stream(allowedExtensions)
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
        
        if (!isExtensionAllowed) {
            log.error("不支持的文件扩展名: {}", extension);
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }
        
        // 第2层：校验文件头（Magic Number），防止文件伪造
        try {
            byte[] fileBytes = file.getBytes();
            String fileType = getFileType(originalFilename).toLowerCase();
            
            // 调试日志：打印文件头前16字节
            StringBuilder hexDump = new StringBuilder();
            for (int i = 0; i < Math.min(16, fileBytes.length); i++) {
                hexDump.append(String.format("%02X ", fileBytes[i]));
            }
            log.info("文件头信息 [{}]: {}", fileType, hexDump.toString());
            
            if (!isValidImageHeader(fileBytes, fileType)) {
                // 检测真实的文件格式
                String actualFormat = detectActualFormat(fileBytes);
                String errorMsg = buildFormatMismatchError(fileType, actualFormat);
                log.error("文件格式不匹配 - 扩展名: {}, 实际格式: {}, 文件头: {}", fileType, actualFormat, hexDump.toString());
                throw new IllegalArgumentException(errorMsg);
            }
            log.debug("文件头校验通过: {}", fileType);
        } catch (IOException e) {
            log.error("读取文件内容失败", e);
            throw new IllegalArgumentException("文件读取失败");
        }
        
        log.info("文件校验通过 - 扩展名: {}, 大小: {} bytes", extension, file.getSize());
    }

    /**
     * 获取文件扩展名(带点)
     */
    private String getFileExtension(String filename) {
        String fileType = getFileType(filename);
        return StringUtils.isNotBlank(fileType) ? "." + fileType : "";
    }
    
    /**
     * 获取文件类型(不带点)
     */
    private String getFileType(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex > 0 && lastDotIndex < filename.length() - 1 ? filename.substring(lastDotIndex + 1) : "";
    }
    
    /**
     * 检测文件的真实格式(基于文件头)
     */
    private String detectActualFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return "未知格式";
        }
            
        // JPEG: FF D8 FF (只需3字节,优先检测)
        if (bytes[0] == (byte)0xFF && bytes[1] == (byte)0xD8 && bytes[2] == (byte)0xFF) {
            return "JPEG";
        }
            
        // PNG: 89 50 4E 47
        if (bytes[0] == (byte)0x89 && bytes[1] == (byte)0x50 && 
            bytes[2] == (byte)0x4E && bytes[3] == (byte)0x47) {
            return "PNG";
        }
            
        // GIF: 47 49 46 38 (GIF8)
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "GIF";
        }
            
        // WebP: RIFF....WEBP (需要至少12字节)
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && 
            bytes[2] == 'F' && bytes[3] == 'F' &&
            bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "WebP";
        }
            
        return "未知格式";
    }
    
    /**
     * 构建格式不匹配的错误提示
     */
    private String buildFormatMismatchError(String declaredFormat, String actualFormat) {
        if ("未知格式".equals(actualFormat) || "未知".equals(actualFormat)) {
            return "文件内容损坏或不是有效的图片格式";
        }
        
        String suggestedExtension;
        switch (actualFormat.toLowerCase()) {
            case "png":
                suggestedExtension = ".png";
                break;
            case "jpeg":
                suggestedExtension = ".jpg 或 .jpeg";
                break;
            case "gif":
                suggestedExtension = ".gif";
                break;
            case "webp":
                suggestedExtension = ".webp";
                break;
            default:
                suggestedExtension = "对应格式";
        }
        
        return String.format(
            "文件格式不匹配：文件扩展名为 .%s，但实际是 %s 格式。请将文件重命名为 %s 后缀后重新上传",
            declaredFormat, actualFormat, suggestedExtension
        );
    }
    
    /**
     * 校验图片文件头（Magic Number）
     * 防止用户将 .exe 等恶意文件改名为 .png 上传
     */
    private boolean isValidImageHeader(byte[] bytes, String fileType) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        
        switch (fileType.toLowerCase()) {
            case "jpg":
            case "jpeg":
                // JPEG: FF D8 FF
                return bytes[0] == (byte)0xFF && 
                       bytes[1] == (byte)0xD8 && 
                       bytes[2] == (byte)0xFF;
            
            case "png":
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                return bytes[0] == (byte)0x89 && 
                       bytes[1] == (byte)0x50 && 
                       bytes[2] == (byte)0x4E && 
                       bytes[3] == (byte)0x47;
            
            case "gif":
                // GIF: 47 49 46 38 (GIF8)
                return bytes[0] == 'G' && 
                       bytes[1] == 'I' && 
                       bytes[2] == 'F' && 
                       bytes[3] == '8';
            
            case "webp":
                // WebP: RIFF....WEBP
                if (bytes.length < 12) return false;
                return bytes[0] == 'R' && 
                       bytes[1] == 'I' && 
                       bytes[2] == 'F' && 
                       bytes[3] == 'F' &&
                       bytes[8] == 'W' && 
                       bytes[9] == 'E' && 
                       bytes[10] == 'B' && 
                       bytes[11] == 'P';
            
            default:
                log.warn("未知的文件类型: {}", fileType);
                return false;
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
