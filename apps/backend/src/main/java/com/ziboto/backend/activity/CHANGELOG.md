# Activity Module Changelog

## [1.0.0] - 2026-08-12

### Added
- ActivityLog entity with 23 activity types
- ActivityType enum (FILE_UPLOADED, FILE_DOWNLOADED, FILE_DELETED, USER_LOGIN, etc.)
- EntityType enum (FILE, FOLDER, USER, SHARE, VERSION, COMMENT, GALLERY, SYSTEM)
- ActivityLogRepository with 20+ custom queries
- ActivityService with async logging using @Async
- ActivityController with 8 REST endpoints:
  - GET /api/v1/activities/user - Get user activities
  - GET /api/v1/activities/global - Get global activities
  - GET /api/v1/activities/file/{fileId} - Get file activities
  - GET /api/v1/activities/folder/{folderId} - Get folder activities
  - GET /api/v1/activities/summary - Get activity summary
  - GET /api/v1/activities/recent - Get recent activities
  - POST /api/v1/activities/cleanup - Cleanup old activities
  - GET /api/v1/activities/{id} - Get specific activity
- ActivityLogResponse DTO with user details
- ActivitySummaryResponse DTO with statistics
- Database migration V17__activity_logs.sql with:
  - activity_logs table with comprehensive fields
  - 8 indexes for query optimization
  - 6 database functions (get_user_activities, get_global_activities, etc.)
  - Automated cleanup of activities older than 90 days

### Features
- Async activity logging for non-blocking operations
- Detailed activity tracking with metadata (JSON)
- IP address tracking for security
- Time-based activity summaries
- Efficient pagination with database functions
- Scheduled cleanup task runs daily

### Database Schema
```sql
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    entity_name VARCHAR(255),
    action VARCHAR(100),
    description TEXT,
    metadata JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);
```

### Performance
- Indexed on user_id, activity_type, entity_type, entity_id, created_at
- Composite index on user_id + created_at for efficient user activity queries
- Database functions for complex queries
- Pagination support for large datasets

### Integration
- Used by all major modules: file, folder, share, auth, gallery, trash
- Provides audit trail for compliance
- Supports filtering by activity type and date range
