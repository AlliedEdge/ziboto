package com.ziboto.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for async event processing.
 * 
 * <p>Defines queues, exchanges, and bindings for:</p>
 * <ul>
 *   <li>File processing (uploads, deletions)</li>
 *   <li>Email notifications</li>
 *   <li>System notifications</li>
 *   <li>Audit events</li>
 * </ul>
 * 
 * <p>Uses JSON message converter for type-safe message handling.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String FILE_UPLOADED_QUEUE = "file.uploaded";
    public static final String FILE_DELETED_QUEUE = "file.deleted";
    public static final String FILE_PROCESSING_QUEUE = "file.processing";
    public static final String EMAIL_QUEUE = "email";
    public static final String NOTIFICATION_QUEUE = "notification";
    public static final String AUDIT_QUEUE = "audit";
    
    // Exchange names
    public static final String FILE_EXCHANGE = "file.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String AUDIT_EXCHANGE = "audit.exchange";
    
    // Routing keys
    public static final String FILE_UPLOADED_KEY = "file.uploaded";
    public static final String FILE_DELETED_KEY = "file.deleted";
    public static final String FILE_PROCESSING_KEY = "file.processing";
    public static final String EMAIL_KEY = "notification.email";
    public static final String NOTIFICATION_KEY = "notification.inapp";
    public static final String AUDIT_KEY = "audit.event";
    
    // Dead letter queues
    public static final String FILE_DLQ = "file.dlq";
    public static final String NOTIFICATION_DLQ = "notification.dlq";
    
    // -------------------------------------------------------------------------
    // File Exchange and Queues
    // -------------------------------------------------------------------------
    
    @Bean
    public TopicExchange fileExchange() {
        return new TopicExchange(FILE_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue fileUploadedQueue() {
        return QueueBuilder.durable(FILE_UPLOADED_QUEUE)
                .withArgument("x-dead-letter-exchange", FILE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "file.dlq")
                .build();
    }
    
    @Bean
    public Queue fileDeletedQueue() {
        return QueueBuilder.durable(FILE_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", FILE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "file.dlq")
                .build();
    }
    
    @Bean
    public Queue fileProcessingQueue() {
        return QueueBuilder.durable(FILE_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", FILE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "file.dlq")
                .build();
    }
    
    @Bean
    public Queue fileDlq() {
        return QueueBuilder.durable(FILE_DLQ).build();
    }
    
    @Bean
    public Binding fileUploadedBinding(Queue fileUploadedQueue, TopicExchange fileExchange) {
        return BindingBuilder.bind(fileUploadedQueue)
                .to(fileExchange)
                .with(FILE_UPLOADED_KEY);
    }
    
    @Bean
    public Binding fileDeletedBinding(Queue fileDeletedQueue, TopicExchange fileExchange) {
        return BindingBuilder.bind(fileDeletedQueue)
                .to(fileExchange)
                .with(FILE_DELETED_KEY);
    }
    
    @Bean
    public Binding fileProcessingBinding(Queue fileProcessingQueue, TopicExchange fileExchange) {
        return BindingBuilder.bind(fileProcessingQueue)
                .to(fileExchange)
                .with(FILE_PROCESSING_KEY);
    }
    
    @Bean
    public Binding fileDlqBinding(Queue fileDlq, TopicExchange fileExchange) {
        return BindingBuilder.bind(fileDlq)
                .to(fileExchange)
                .with("file.dlq");
    }
    
    // -------------------------------------------------------------------------
    // Notification Exchange and Queues
    // -------------------------------------------------------------------------
    
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }
    
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }
    
    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }
    
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(notificationExchange)
                .with(EMAIL_KEY);
    }
    
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_KEY);
    }
    
    @Bean
    public Binding notificationDlqBinding(Queue notificationDlq, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationDlq)
                .to(notificationExchange)
                .with("notification.dlq");
    }
    
    // -------------------------------------------------------------------------
    // Audit Exchange and Queue
    // -------------------------------------------------------------------------
    
    @Bean
    public FanoutExchange auditExchange() {
        return new FanoutExchange(AUDIT_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE).build();
    }
    
    @Bean
    public Binding auditBinding(Queue auditQueue, FanoutExchange auditExchange) {
        return BindingBuilder.bind(auditQueue).to(auditExchange);
    }
    
    // -------------------------------------------------------------------------
    // Message Converter and Template
    // -------------------------------------------------------------------------
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
