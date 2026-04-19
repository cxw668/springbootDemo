package demo.task;

import demo.common.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件清理定时任务
 * 功能：定期清理过期的上传文件，释放磁盘空间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupJob {

    private final AppProperties appProperties;

    /**
     * 文件清理任务
     * 默认每天凌晨2点执行，可通过配置 app.job.file-cleanup-cron 修改
     */
    @Scheduled(cron = "${app.job.file-cleanup-cron}")
    public void cleanExpiredFiles() {
        log.info("========== 开始执行文件清理任务 ==========");
        
        try {
            // 1. 获取配置参数
            String uploadDir = appProperties.getFileUpload().getUploadDir();
            int retentionDays = appProperties.getJob().getFileRetentionDays();
            
            log.info("   - 上传目录: {}", uploadDir);
            log.info("   - 保留天数: {} 天", retentionDays);
            
            // 2. 计算过期时间点
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            log.info("   - 清理截止时间: {}", cutoffTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 3. 执行清理
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists() || !uploadFolder.isDirectory()) {
                log.warn("上传目录不存在或不是有效目录: {}", uploadDir);
                return;
            }
            
            int[] deletedCount = {0};
            long[] deletedSize = {0};
            
            // 递归清理子目录
            cleanDirectory(uploadFolder, cutoffTime, deletedCount, deletedSize);
            
            // 4. 输出清理结果
            log.info("🗑️  文件清理完成");
            log.info("   - 删除文件数: {}", deletedCount[0]);
            log.info("   - 释放空间: {} KB", deletedSize[0] / 1024);
            log.info("========== 文件清理任务执行完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 文件清理任务执行失败", e);
        }
    }
    
    /**
     * 递归清理目录中的过期文件
     * 
     * @param directory 目录
     * @param cutoffTime 截止时间
     * @param deletedCount 删除计数（数组用于引用传递）
     * @param deletedSize 删除文件大小（数组用于引用传递）
     */
    private void cleanDirectory(File directory, LocalDateTime cutoffTime, int[] deletedCount, long[] deletedSize) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                cleanDirectory(file, cutoffTime, deletedCount, deletedSize);
                
                // 如果子目录为空，则删除该目录
                File[] remainingFiles = file.listFiles();
                if (remainingFiles != null && remainingFiles.length == 0) {
                    if (file.delete()) {
                        log.debug("删除空目录: {}", file.getAbsolutePath());
                    }
                }
            } else {
                try {
                    // 检查文件最后修改时间（使用时间戳比较，性能更优且避免时区问题）
                    long fileLastModified = file.lastModified();
                    long cutoffTimestamp = LocalDateTime.now()
                            .minusDays(cutoffTime.getDayOfMonth())
                            .minusHours(cutoffTime.getHour())
                            .minusMinutes(cutoffTime.getMinute())
                            .minusSeconds(cutoffTime.getSecond())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                    
                    if (fileLastModified < cutoffTimestamp) {
                        long fileSize = file.length();
                        if (file.delete()) {
                            deletedCount[0]++;
                            deletedSize[0] += fileSize;
                            log.debug("删除过期文件: {} (修改时间: {}, 大小: {} bytes)", 
                                    file.getName(), 
                                    new java.util.Date(fileLastModified),
                                    fileSize);
                        } else {
                            log.warn("删除文件失败: {}", file.getAbsolutePath());
                        }
                    }
                } catch (SecurityException e) {
                    log.warn("无权限访问文件，跳过: {}, 错误: {}", file.getAbsolutePath(), e.getMessage());
                } catch (Exception e) {
                    log.warn("处理文件时发生异常，跳过: {}, 错误: {}", file.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}
