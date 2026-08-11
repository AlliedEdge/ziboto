# Changelog

All notable changes to the Ziboto project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2026-08-12

### Added - V3: User Experience Enhancement
- **Activity Feed System**
  - Activity logging with 23 activity types
  - User activity streams with filtering
  - Global activity feed for admins
  - Entity-specific activity tracking
  - Activity summaries and statistics
  - Scheduled cleanup of old activities

- **Real-Time Collaboration**
  - WebSocket support with STOMP protocol
  - Real-time notifications for file events
  - User online/offline status tracking
  - SockJS fallback for browser compatibility
  - JWT authentication for WebSocket connections

- **File Commenting System**
  - Threaded comments on files
  - Comment editing and deletion
  - User mentions in comments
  - Comment reply tracking
  - Edit tracking with timestamps

- **Trash Bin Feature**
  - Soft delete for files and folders
  - 30-day retention policy
  - Restore from trash functionality
  - Permanent deletion
  - Empty trash bulk operation
  - Scheduled auto-cleanup of old items

- **Storage Analytics**
  - Storage overview dashboard
  - File type breakdown analysis
  - Storage usage trends over time
  - Most accessed files tracking
  - Activity by day statistics
  - Automated snapshot recording

- **Public Galleries**
  - Create shareable file collections
  - Multiple themes (default, dark, light, minimal, vibrant)
  - Multiple layouts (grid, masonry, slideshow, list)
  - Password protection for galleries
  - View count tracking
  - File reordering in galleries
  - Unique slug generation

- **File Preview System**
  - Preview generation for multiple formats
  - Image previews with thumbnails
  - PDF preview support
  - Video preview/thumbnails
  - Audio waveform visualization
  - Document preview (Word, Excel, etc.)
  - Code syntax highlighting
  - Preview caching system
  - Scheduled cleanup of expired previews

### Changed
- Updated error codes to include UNAUTHORIZED and DUPLICATE_RESOURCE
- Enhanced FileMetadata entity with preview fields
- Improved entity relationships and database indexes

### Database
- Migration V17: Activity logs table with comprehensive indexing
- Migration V18: File comments with threading support
- Migration V19: Trash bin with soft delete columns
- Migration V20: Storage analytics history
- Migration V21: Public galleries and gallery_files tables
- Migration V22: File previews table and caching

## [2.0.0] - 2026-08-09

### Added - V2: Feature Maturity
- **Advanced Authentication**
  - Email verification workflow
  - Google OAuth integration
  - Refresh token rotation
  - Password reset functionality
  - Account security features

- **File Sharing**
  - Public/private share links
  - Expirable share links
  - Password-protected shares
  - Share permissions (view/download/edit)
  - Share revocation

- **File Versioning**
  - Version history tracking
  - Restore previous versions
  - Version comparison
  - Version deletion

- **Search System**
  - Advanced search by name, type, size, date
  - Elasticsearch integration
  - Full-text search capabilities
  - Search filters and sorting

- **Duplicate Detection**
  - SHA-256 hash-based detection
  - Storage optimization
  - Duplicate file linking

- **Background Processing**
  - RabbitMQ message queue
  - Asynchronous file processing
  - Thumbnail generation
  - Email notifications
  - Metadata extraction

- **Audit System**
  - Comprehensive activity logging
  - Security event tracking
  - User action history
  - System event monitoring

- **Role-Based Access Control**
  - User roles (USER, ADMIN, SUPER_ADMIN)
  - Permission-based access
  - Resource ownership validation

### Changed
- Enhanced security with RBAC
- Improved file metadata structure
- Optimized database queries with indexes

### Database
- Migrations V8-V16: All V2 features (sharing, versioning, audit, notifications, etc.)

## [1.0.0] - 2026-08-06

### Added - V1: MVP
- **Core File Management**
  - File upload with multipart support
  - File download with streaming
  - Hierarchical folder structure
  - File metadata management
  - Basic CRUD operations

- **Authentication**
  - User registration and login
  - JWT-based authentication
  - Password encryption with BCrypt
  - Session management

- **Storage Integration**
  - AWS S3 object storage
  - Local storage fallback
  - File metadata in PostgreSQL
  - S3 key generation

- **Caching**
  - Redis integration
  - Cache configuration
  - Frequently accessed data caching

- **Infrastructure**
  - Docker containerization
  - Docker Compose orchestration
  - Nginx reverse proxy
  - PostgreSQL database
  - Redis cache
  - Health check endpoints

- **API Documentation**
  - Swagger/OpenAPI integration
  - Interactive API documentation
  - Request/response examples

### Database
- Migrations V1-V7: Initial schema (users, folders, files, metadata)

---

## Versioning Strategy

- **v1 (MVP)**: Core storage functionality with basic deployment
- **v2 (Feature Maturity)**: Advanced features while maintaining v1's architecture
- **v3 (UX Enhancement)**: Activity tracking, real-time features, content capabilities
- **v4 (Cloud-Native)**: Infrastructure upgrade to Kubernetes, Terraform, monitoring
- **v5 (Intelligence & Mobile)**: AI features, mobile apps, collaboration

## Links

- [GitHub Repository](https://github.com/yourusername/ziboto)
- [Documentation](https://github.com/yourusername/ziboto/wiki)
- [Issue Tracker](https://github.com/yourusername/ziboto/issues)
