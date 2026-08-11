# Analytics Module Changelog

## [1.0.0] - 2026-08-12

### Added
- AnalyticsService using JdbcTemplate for complex queries
- AnalyticsController with 2 REST endpoints:
  - GET /api/v1/analytics/storage - Get storage analytics
  - POST /api/v1/analytics/snapshot - Record storage snapshot
- StorageAnalyticsResponse DTO with nested classes:
  - StorageOverview (total files, size, quota, percentage)
  - FileTypeBreakdown (by extension with counts and percentages)
  - StorageTrend (usage over time)
  - MostAccessedFile (top downloaded files)
  - ActivityByDay (uploads/downloads per day)
- Database migration V20__storage_analytics.sql with:
  - storage_usage_history table
  - 3 indexes for historical queries
  - 5 database functions for analytics

### Features
- **Storage Overview**
  - Total file count
  - Total storage used
  - Storage quota limit
  - Usage percentage
  - Available storage

- **File Type Analysis**
  - Breakdown by file extension
  - File count per type
  - Total size per type
  - Percentage of total storage

- **Usage Trends**
  - Storage usage over time
  - Date-based trend analysis
  - Historical snapshots

- **Access Analytics**
  - Most downloaded files
  - Download count tracking
  - File popularity metrics

- **Activity Statistics**
  - Uploads per day
  - Downloads per day
  - Date-based activity tracking

### Database Schema
```sql
CREATE TABLE storage_usage_history (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_files BIGINT,
    total_size BIGINT,
    storage_quota BIGINT,
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

### Database Functions
- `get_storage_by_file_type(user_id)` - File type breakdown
- `get_storage_usage_trend(user_id, days)` - Usage over time
- `get_most_accessed_files(user_id, limit)` - Top downloaded files
- `get_activity_by_day(user_id, days)` - Daily activity stats
- `record_storage_snapshot(user_id)` - Create usage snapshot

### Performance
- Indexed on user_id and snapshot_date
- Efficient aggregation with database functions
- JdbcTemplate for optimized query execution
- Minimal application-level processing

### Security
- Authentication required
- User-scoped analytics only
- No cross-user data exposure

### Integration
- Uses FileMetadataRepository for file data
- User entity for quota information
- Activity tracking for download statistics

### Future Enhancements
- Scheduled snapshot recording (daily/weekly)
- Predictive storage forecasting
- Storage optimization recommendations
- Cost analysis (S3 storage costs)
