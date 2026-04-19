package demo.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank(message = "应用名称不能为空！")
    private String name;

    private String version = "1.0.0";

    private String description;

    @Min(value = 1,  message = "分页大小最小为1！")
    @Max(value = 100, message = "分页大小最大为100！")
    private  Integer defaultPageSize = 10;

    @Min(value = 1, message = "最大分页大小最小为 1")
    @Max(value = 500, message = "最大分页大小最大为 500")
    private Integer maxPageSize = 100;

    private Cors cors = new Cors();

    private FileUpload fileUpload = new FileUpload();

    private Job job = new Job();

    @Data
    public static class Cors {
        private String allowedOrigins = "*";
        private String[] allowedMethods = new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"};
        private Long maxAge = 3600L;
    }

    @Data
    public static class FileUpload {
        // 上传目录
        private String uploadDir = "upload";
        // 最大文件大小 10MB
        private Long maxFileSize = 1024 * 1024 * 10L;
        // 文件类型
        private String[] allowedFileTypes = new String[]{"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        // 文件扩展名
        private String[] allowedFileExtensions = new String[]{".jpg", ".jpeg", ".png", ".gif", ".webp"};
    }

    @Data
    public static class Messaging {
        private Boolean enabled = false;
    }

    @Data
    public static class Job {
        // 用户统计任务 Cron 表达式
        private String userStatsCron = "0 0 8 * * ?";
        // 文件清理任务 Cron 表达式
        private String fileCleanupCron = "0 0 2 * * ?";
        // 文件保留天数
        private Integer fileRetentionDays = 7;
    }
}
