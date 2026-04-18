package demo.messaging.listener;

import demo.config.MessageQueueConfig;
import demo.messaging.MessageDedupService;
import demo.messaging.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class UserRegisteredEventListener {

    private final MessageDedupService messageDedupService;

    @RabbitListener(queues = MessageQueueConfig.USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        if (messageDedupService.isProcessed(event.getEventId())) {
            log.warn("检测到重复的 user.registered 事件，跳过处理: eventId={}", event.getEventId());
            return;
        }

        if (!messageDedupService.tryStartProcessing(event.getEventId())) {
            log.warn("user.registered 事件正在处理中，跳过重复投递: eventId={}", event.getEventId());
            return;
        }

        try {
            if (event.getUsername() != null && event.getUsername().startsWith("fail-mq-")) {
                throw new IllegalStateException("模拟 user.registered 消费失败，消息将进入 DLQ");
            }

            log.info("异步发送欢迎通知: userId={}, username={}, email={}",
                    event.getUserId(), event.getUsername(), event.getEmail());
            log.info("异步记录注册审计日志: eventId={}, occurredAt={}",
                    event.getEventId(), event.getOccurredAt());
            messageDedupService.markProcessed(event.getEventId());
        } catch (RuntimeException ex) {
            messageDedupService.clearProcessing(event.getEventId());
            throw ex;
        }
    }

    @RabbitListener(queues = MessageQueueConfig.USER_REGISTERED_DLQ)
    public void handleUserRegisteredDlq(UserRegisteredEvent event,
                                        @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
        log.error("user.registered 死信消息，请人工排查: eventId={}, routingKey={}, payload={}",
                event.getEventId(), routingKey, event);
    }
}
