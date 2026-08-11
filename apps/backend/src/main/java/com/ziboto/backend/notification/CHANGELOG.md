# Changelog - Notification Module

All notable changes to the notification module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- Notification system for user events
- NotificationService for creating notifications
- Notification persistence
- Email notification integration
- Notification types:
  - FILE_SHARED
  - FOLDER_SHARED
  - SHARE_EXPIRED
  - COMMENT_ADDED (V3)
  - COMMENT_REPLY (V3)

### Features
- In-app notifications
- Email notifications
- Notification read/unread status
- Notification history
- Notification preferences (future)

### Database
- notifications table
- User ID, type, message, link
- Created timestamp
- Read timestamp

### API (Planned)
- Get user notifications
- Mark as read
- Mark all as read
- Delete notification
- Get unread count

### Future Enhancements (V5)
- Push notifications (mobile)
- WebSocket real-time notifications
- Notification preferences
- Notification grouping
- Notification badges
