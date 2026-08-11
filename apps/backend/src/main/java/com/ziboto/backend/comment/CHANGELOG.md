# Comment Module Changelog

## [1.0.0] - 2026-08-12

### Added
- FileComment entity with threading support
- CommentRequest DTO for creating/updating comments
- CommentResponse DTO with user details and reply count
- FileCommentRepository with custom queries
- CommentService with full CRUD operations
- CommentController with 6 REST endpoints:
  - POST /api/v1/comments/files/{fileId} - Add comment
  - GET /api/v1/comments/files/{fileId} - Get file comments
  - PUT /api/v1/comments/{commentId} - Update comment
  - DELETE /api/v1/comments/{commentId} - Delete comment
  - GET /api/v1/comments/files/{fileId}/count - Get comment count
  - GET /api/v1/comments/mentions - Get mentions
- Database migration V18__file_comments.sql with:
  - file_comments table
  - Self-referencing parent_id for threading
  - JSONB mentions array
  - 6 indexes for query optimization
  - get_file_comments database function with recursive query

### Features
- **Threaded Comments**: Support for nested replies with parent_id
- **User Mentions**: Tag users with @username (stored as JSONB array of user IDs)
- **Edit Tracking**: is_edited flag with updated_at timestamp
- **Reply Counting**: Automatic reply count calculation
- **User Details**: Includes username and avatar in responses
- **Time Formatting**: Human-readable time ago (5m ago, 2h ago, etc.)
- **Pagination**: Page-based comment loading
- **Ownership Validation**: Users can only edit/delete their own comments

### Database Schema
```sql
CREATE TABLE file_comments (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id UUID, -- for threading
    content TEXT NOT NULL,
    mentions JSONB, -- array of user IDs
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Performance
- Indexed on file_id, user_id, parent_id, created_at
- Database function for recursive comment tree loading
- Efficient pagination with Spring Data

### Security
- Authentication required for all endpoints
- Ownership validation for edit/delete operations
- Content validation and sanitization

### Integration
- Integrated with activity logging
- User profile lookup for comment authors
- Notification support for mentions (future)
