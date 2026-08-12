# Changelog - Messaging Module

All notable changes to the messaging/queue module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- RabbitMQ integration
- Message queue configuration
- Async task processing
- Email notification queue
- File processing queue

### Queues
- **email.queue**: Email sending tasks
- **file.processing.queue**: File upload processing
- **notification.queue**: User notifications

### Features
- Dead letter queue (DLQ) for failed messages
- Message retry with exponential backoff
- Message persistence
- Queue durability
- Consumer acknowledgment

### Configuration
- RabbitMQ connection factory
- Queue declaration
- Exchange configuration
- Binding setup
- Message converter (JSON)

### Use Cases
- Background email sending
- Async file processing
- Notification delivery
- Audit log processing (V3)
- Analytics snapshot generation (V3)
