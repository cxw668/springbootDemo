package demo.messaging.listener;

import demo.config.MessageQueueConfig;
import demo.messaging.MessageDedupService;
import demo.messaging.event.FileUploadedEvent;
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
public class FileUploadedEventListener {

    private final MessageDedupService messageDedupService;

    @RabbitListener(queues = MessageQueueConfig.FILE_UPLOADED_QUEUE)
    public void handleFileUploaded(FileUploadedEvent event) {
        if (messageDedupService.isProcessed(event.getEventId())) {
            log.warn("检测到重复的 file.uploaded 事件，跳过处理: eventId={}", event.getEventId());
            return;
        }

        if (!messageDedupService.tryStartProcessing(event.getEventId())) {
            log.warn("file.uploaded 事件正在处理中，跳过重复投递: eventId={}", event.getEventId());
            return;
        }

        try {
            if ("simulate-fail".equalsIgnoreCase(event.getSubDir())) {
                throw new IllegalStateException("模拟 file.uploaded 消费失败，消息将进入 DLQ");
            }

            log.info("异步处理文件上传事件: filePath={}, originalFilename={}, size={}",
                    event.getFilePath(), event.getOriginalFilename(), event.getSize());
            log.info("执行后处理任务: 提取元数据 / 生成缩略图 / 调用后续处理流水线");
            messageDedupService.markProcessed(event.getEventId());
        } catch (RuntimeException ex) {
            messageDedupService.clearProcessing(event.getEventId());
            throw ex;
        }
    }

    @RabbitListener(queues = MessageQueueConfig.FILE_UPLOADED_DLQ)
    public void handleFileUploadedDlq(FileUploadedEvent event,
                                      @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
        log.error("file.uploaded 死信消息，请人工排查: eventId={}, routingKey={}, payload={}",
                event.getEventId(), routingKey, event);
    }
}
