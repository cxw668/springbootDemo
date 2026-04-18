package demo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class MessageQueueConfig {

    public static final String EVENT_EXCHANGE = "app.event.exchange";
    public static final String DLX_EXCHANGE = "app.event.dlx";

    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
    public static final String USER_REGISTERED_DLQ = "user.registered.dlq";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_REGISTERED_DLQ_ROUTING_KEY = "user.registered.dlq";

    public static final String FILE_UPLOADED_QUEUE = "file.uploaded.queue";
    public static final String FILE_UPLOADED_DLQ = "file.uploaded.dlq";
    public static final String FILE_UPLOADED_ROUTING_KEY = "file.uploaded";
    public static final String FILE_UPLOADED_DLQ_ROUTING_KEY = "file.uploaded.dlq";

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // 持久化队列：死信机制、队列持久化、指定路由键
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(USER_REGISTERED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable(USER_REGISTERED_DLQ).build();
    }

    /**
     * 绑定用户注册队列到主题交换机，使用指定的路由键
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(eventExchange)
                .with(USER_REGISTERED_ROUTING_KEY);
    }

    /**
     * 绑定用户注册死信队列到死信交换机，使用指定的死信路由键
     */
    @Bean
    public Binding userRegisteredDlqBinding(Queue userRegisteredDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(userRegisteredDlq)
                .to(deadLetterExchange)
                .with(USER_REGISTERED_DLQ_ROUTING_KEY);
    }

    /**
     * 创建文件上传队列，配置死信交换机和死信路由键
     */
    @Bean
    public Queue fileUploadedQueue() {
        return QueueBuilder.durable(FILE_UPLOADED_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(FILE_UPLOADED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue fileUploadedDlq() {
        return QueueBuilder.durable(FILE_UPLOADED_DLQ).build();
    }

    @Bean
    public Binding fileUploadedBinding(Queue fileUploadedQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(fileUploadedQueue)
                .to(eventExchange)
                .with(FILE_UPLOADED_ROUTING_KEY);
    }

    @Bean
    public Binding fileUploadedDlqBinding(Queue fileUploadedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(fileUploadedDlq)
                .to(deadLetterExchange)
                .with(FILE_UPLOADED_DLQ_ROUTING_KEY);
    }
}
