# Trash Module Changelog

## [1.0.0] - 2026-08-12

### Added
- TrashService with soft delete functionality
- TrashController with 6 REST endpoints:
  - GET /api/v1/trash - Get trash items
  - POST /api/v1/trash/files/{fileId} - Move file to trash
  - POST /api/v1/trash/folders/{folderId} - Move folder to trash
  - POST /api/v1/trash/restore/files/{fileId} - Restore file
  - POST /api/v1/trash/restore/folders/{folderId} - Restore folder
  - DELETE /api/v1/trash/files/{fileId} - Permanently delete file
  - DELETE /api/v1/trash/folders/{folderId} - Permanently delete folder
  - POST /api/v1/trash/empty - Empty entire trash
  - GET /api/v1/trash/summary - Get trash summary
- TrashItemResponse DTO with type, size, and deletion info
- TrashSummaryResponse DTO with statistics
- TrashItemType enum (FILE, FOLDER)
- Database migration V19__trash_bin.sql with:
  - deleted_at and deleted_by columns added to file_metadata and folders
  - 4 indexes for trash queries
  - 6 database functions for trash operations

### Features
- **30-Day Retention**: Items kept in trash for 30 days before auto-deletion
- **Soft Delete**: Files/folders marked as deleted without actual removal
- **Restore Capability**: Full restoration with original location
- **Permanent Delete**: Immediate removal from trash
- **Empty Trash**: Bulk deletion of all trash items
- **Storage Reclamation**: S3 object deletion on permanent delete
- **Scheduled Cleanup**: Daily task at 2 AM to remove old items (30+ days)
- **Deletion Tracking**: Records who deleted items and when

### Database Schema
```sql
ALTER TABLE file_metadata ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE file_metadata ADD COLUMN deleted_by VARCHAR(255);
ALTER TABLE folders ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE folders ADD COLUMN deleted_by VARCHAR(255);
```

### Performance
- Indexed on deleted_at for efficient trash queries
- Database functions for complex restore operations
- Scheduled task with @Scheduled annotation
- Pagination support for trash listing

### Security
- Authentication required for all operations
- Ownership validation before deletion/restoration
- Audit logging for all trash operations

### Storage Management
- Automatic S3 object deletion on permanent delete
- Storage quota updates on deletion/restoration
- Folder hierarchy handling for nested deletions

### Integration
- Integrated with activity logging
- Storage service for S3 operations
- Folder service for hierarchy management
- Updates user storage usage statistics

### Scheduled Tasks
- **Daily Cleanup**: Runs at 2 AM (cron: 0 0 2 * * *)
- Removes items deleted more than 30 days ago
- Logs cleanup statistics
