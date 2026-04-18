package demo.messaging;

import demo.config.MessageQueueConfig;
import demo.messaging.event.FileUploadedEvent;
import demo.messaging.event.UserRegisteredEvent;
import demo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitMqDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                LocalDateTime.now()
        );

        rabbitTemplate.convertAndSend(
                MessageQueueConfig.EVENT_EXCHANGE,
                MessageQueueConfig.USER_REGISTERED_ROUTING_KEY,
                event
        );

        log.info("已发布 user.registered 事件: eventId={}, userId={}", event.getEventId(), event.getUserId());
    }

    @Override
    public void publishFileUploaded(Long userId, String subDir, String filePath, MultipartFile file) {
        FileUploadedEvent event = new FileUploadedEvent(
                UUID.randomUUID().toString(),
                userId,
                filePath,
                subDir,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                LocalDateTime.now()
        );

        rabbitTemplate.convertAndSend(
                MessageQueueConfig.EVENT_EXCHANGE,
                MessageQueueConfig.FILE_UPLOADED_ROUTING_KEY,
                event
        );

        log.info("已发布 file.uploaded 事件: eventId={}, filePath={}", event.getEventId(), event.getFilePath());
    }
}
