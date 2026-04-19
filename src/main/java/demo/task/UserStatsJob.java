package demo.task;

import demo.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户统计定时任务
 * 功能：每日统计系统用户数量并生成日报
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatsJob {

    private final IUserService userService;

    /**
     * 每日用户统计任务
     * 默认每天早上8点执行，可通过配置 app.job.user-stats-cron 修改
     */
    @Scheduled(cron = "${app.job.user-stats-cron}")
    public void generateDailyUserStats() {
        log.info("========== 开始执行每日用户统计任务 ==========");
        
        try {
            // 1. 获取总用户数
            long totalUsers = userService.count();
            
            // 2. 获取今日新增用户数（昨天0点到今天0点之间注册的用户）
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.plusDays(1).atStartOfDay();
            
            long todayNewUsers = userService.lambdaQuery()
                    .ge(demo.model.User::getCreateTime, startOfDay)
                    .lt(demo.model.User::getCreateTime, endOfDay)
                    .count();
            
            // 3. 格式化日期
            String reportDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 4. 生成日报日志
            log.info("📊 用户统计日报 [{}]", reportDate);
            log.info("   - 系统总用户数: {}", totalUsers);
            log.info("   - 昨日新增用户: {}", todayNewUsers);
            log.info("   - 统计时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // TODO: 后续可以将日报数据存入数据库或发送到通知渠道
            
            log.info("========== 每日用户统计任务执行完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 用户统计任务执行失败", e);
        }
    }
}
