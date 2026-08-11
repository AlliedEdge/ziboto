package com.ziboto.backend.messaging.publisher;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.ziboto.backend.config.RabbitMQConfig;
import com.ziboto.backend.messaging.event.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for publishing events to RabbitMQ.
 * 
 * <p>Provides high-level methods for publishing domain events
 * without exposing RabbitMQ details to business logic.</p>
 * 
 * <p>All events are published with:</p>
 * <ul>
 *   <li>Unique event ID</li>
 *   <li>Timestamp</li>
 *   <li>JSON serialization</li>
 *   <li>Persistent delivery mode</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    // -------------------------------------------------------------------------
    // File Events
    // -------------------------------------------------------------------------
    
    /**
     * Publish file uploaded event.
     * 
     * @param event file uploaded event
     */
    public void publishFileUploaded(FileUploadedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.FILE_EXCHANGE,
                RabbitMQConfig.FILE_UPLOADED_KEY,
                event
            );
            log.info("Published file uploaded event: fileId={}, eventId={}", 
                     event.getFileId(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish file uploaded event: fileId={}", 
                      event.getFileId(), e);
            // Don't throw - event publishing should not break main flow
        }
    }
    
    /**
     * Publish file deleted event.
     * 
     * @param event file deleted event
     */
    public void publishFileDeleted(FileDeletedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.FILE_EXCHANGE,
                RabbitMQConfig.FILE_DELETED_KEY,
                event
            );
            log.info("Published file deleted event: fileId={}, eventId={}", 
                     event.getFileId(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish file deleted event: fileId={}", 
                      event.getFileId(), e);
        }
    }
    
    // -------------------------------------------------------------------------
    // Notification Events
    // -------------------------------------------------------------------------
    
    /**
     * Publish email event for async email sending.
     * 
     * @param event email event
     */
    public void publishEmail(EmailEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        if (event.getRetryCount() == null) {
            event.setRetryCount(0);
        }
        
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.EMAIL_KEY,
                event
            );
            log.info("Published email event: type={}, to={}, eventId={}", 
                     event.getEmailType(), event.getToEmail(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish email event: type={}, to={}", 
                      event.getEmailType(), event.getToEmail(), e);
        }
    }
    
    /**
     * Publish notification event for in-app notifications.
     * 
     * @param event notification event
     */
    public void publishNotification(NotificationEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_KEY,
                event
            );
            log.info("Published notification event: type={}, userId={}, eventId={}", 
                     event.getType(), event.getUserId(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish notification event: type={}, userId={}", 
                      event.getType(), event.getUserId(), e);
        }
    }
    
    // -------------------------------------------------------------------------
    // Audit Events
    // -------------------------------------------------------------------------
    
    /**
     * Publish audit event.
     * 
     * @param event audit event
     */
    public void publishAudit(Object event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.AUDIT_EXCHANGE,
                "",  // Fanout exchange ignores routing key
                event
            );
            log.debug("Published audit event: event={}", event.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }
}
