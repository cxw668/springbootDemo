package demo.messaging;

import demo.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publishUserRegistered(User user) {
        log.debug("消息队列未启用，跳过 user.registered 事件: {}", user != null ? user.getUsername() : null);
    }

    @Override
    public void publishFileUploaded(Long userId, String subDir, String filePath, MultipartFile file) {
        log.debug("消息队列未启用，跳过 file.uploaded 事件: {}", filePath);
    }
}
