# Changelog - Backend

All notable changes to the Ziboto Backend application are documented here.

## [3.0.0] - 2026-08-12

### Added - V3: User Experience Enhancement
- **Activity Feed System**
  - ActivityLog entity with 23 activity types
  - ActivityService with async logging
  - ActivityController with 8 REST endpoints
  - Database migration V17 with activity tracking
  - Activity summaries and statistics
  - Scheduled cleanup of old activities (90+ days)

- **Real-Time Collaboration** (WebSocket - temporarily disabled pending dependency setup)
  - WebSocket configuration with STOMP protocol
  - WebSocketService for real-time notifications
  - WebSocketController for connection management
  - JWT authentication for WebSocket connections
  - SockJS fallback support
  - Message types for file events and user status

- **File Commenting System**
  - FileComment entity with threading support
  - CommentService with CRUD operations
  - CommentController with 6 REST endpoints
  - Database migration V18 with comments table
  - Support for mentions (@user)
  - Edit tracking and reply counting

- **Trash Bin Feature**
  - TrashService with soft delete functionality
  - TrashController with 6 REST endpoints
  - Database migration V19 adding deleted_at/deleted_by columns
  - 30-day retention policy
  - Scheduled auto-cleanup task (@Scheduled daily at 2 AM)
  - Restore functionality for files and folders

- **Storage Analytics**
  - AnalyticsService using JdbcTemplate
  - AnalyticsController with 2 REST endpoints
  - Database migration V20 with storage_usage_history table
  - Storage overview dashboard
  - File type breakdown analysis
  - Usage trends over time
  - Most accessed files tracking

- **Public Galleries**
  - Gallery and GalleryFile entities
  - GalleryService with full CRUD operations
  - GalleryController with 9 REST endpoints
  - Database migration V21 with galleries tables
  - Multiple themes (default, dark, light, minimal, vibrant)
  - Multiple layouts (grid, masonry, slideshow, list)
  - Password protection support
  - View count tracking
  - Unique slug generation

- **File Preview System**
  - FilePreview entity for caching
  - PreviewType enum (THUMBNAIL, IMAGE, PDF, VIDEO, AUDIO, DOCUMENT, CODE)
  - PreviewStatus enum (PENDING, PROCESSING, COMPLETED, FAILED, NOT_SUPPORTED)
  - Database migration V22 with file_previews table
  - Preview expiration and cleanup
  - FilePreviewRepository with preview queries

### Changed
- Enhanced FileMetadata entity with new fields:
  - s3Key, contentType, thumbnailUrl, previewUrl, deletedBy
  - Added getFilename(), getFileName(), getS3Key(), getContentType() methods
- Updated User entity with getProfilePicture() alias method
- Enhanced ErrorCode with UNAUTHORIZED and DUPLICATE_RESOURCE constants
- Fixed FolderRepository with both Page and List method variants
- Fixed FileMetadataRepository duplicate method issues
- Fixed TrashService imports (corrected package paths)

### Database Migrations
- V17: Activity logs with comprehensive activity tracking
- V18: File comments with threading and mentions
- V19: Trash bin with soft delete columns
- V20: Storage analytics and usage history
- V21: Public galleries with multiple features
- V22: File preview system with caching

### Fixed
- Resolved 100+ compilation errors from V3 implementation
- Fixed Lombok annotation issues across entities
- Corrected repository method signatures
- Fixed entity field access issues

## [2.0.0] - 2026-08-09

### Added - V2: Feature Maturity
- Email verification system
- Google OAuth integration
- File sharing with expirable/password-protected links
- File versioning and history
- Elasticsearch full-text search
- Duplicate detection (SHA-256)
- RabbitMQ background processing
- Comprehensive audit logging
- Role-based access control (RBAC)
- Notification system
- Database migrations V8-V16

## [Unreleased] - Previously Documented

### Added
- File management REST API with FileController
- Folder management REST API with FolderController
- FileService for file operations (upload, download, delete, list, search)
- FolderService for folder operations (create, rename, move, delete)
- StorageService abstraction layer
- LocalStorageService implementation for file system storage
- File and folder DTOs (FileMetadataResponse, FileUploadResponse, FolderRequest, FolderResponse)
- Folder entity with hierarchical structure support
- FolderRepository for database operations
- DotenvConfig for .env file support
- PowerShell version of JWT secret generation script

### Changed
- Updated pom.xml dependencies
- Modified application.yml for production readiness
- Enhanced authentication and authorization logic
- Improved error handling and validation

## [0.2.0] - 2026-08-05

### Added - V1: MVP
- Complete Spring Boot application structure
- JWT authentication and authorization system
- User registration and login REST APIs
- Refresh token mechanism with rotation
- Redis integration for caching and session management
- Audit logging system
- User management module
- Authentication module with comprehensive security
- File storage module (initial)
- Security configuration
- Common utilities and exception handling
- Flyway database migrations V1-V7
- Maven configuration with all dependencies
- Application configuration files
- Development and deployment scripts

### Security
- JWT authentication with configurable expiration
- Password encryption with BCrypt
- CSRF protection
- CORS configuration
- Rate limiting per endpoint
- Failed login attempt tracking
- Account lockout mechanism
- Token blacklist for logout
- Session management with Redis
- Security headers

---

## Versioning Strategy

- **v1**: MVP with core storage functionality
- **v2**: Feature maturity (sharing, versioning, search, notifications)
- **v3**: User experience (activity, comments, analytics, galleries, previews)
- **v4**: Cloud-native infrastructure (Kubernetes, Terraform)
- **v5**: Intelligence & mobile (AI, mobile apps, collaboration)

## Technology Stack

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL 15+
- Redis 7+
- RabbitMQ 3.x
- AWS S3
- Elasticsearch 8.x (disabled by user)
- Docker & Docker Compose
