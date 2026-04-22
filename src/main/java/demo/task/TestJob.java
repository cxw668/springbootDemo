package demo.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 测试定时任务
 * 用于验证 @EnableScheduling 是否正常工作
 */
@Slf4j
@Component
public class TestJob {

    /**
     * 每30秒执行一次的测试任务
     */
    // @Scheduled(fixedRate = 30000)
    public void testTask() {
        log.info("⏰ 定时任务测试 - 当前时间: {}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
