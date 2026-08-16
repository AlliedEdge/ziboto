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

## [4.0.0] - 2026-08-12

### Added - V4: Cloud-Native Infrastructure
- **Kubernetes Orchestration**
  - Complete Kubernetes manifests for production deployment
  - Namespace isolation with resource quotas
  - ConfigMaps for non-sensitive configuration
  - Secrets management for sensitive data
  - StatefulSets for PostgreSQL, Redis, and RabbitMQ
  - Deployments for backend (Spring Boot) and frontend (React)
  - Services (ClusterIP) for internal communication
  - Ingress with NGINX controller and SSL/TLS termination
  - Horizontal Pod Autoscaler (HPA) for auto-scaling
  - PersistentVolumeClaims for data persistence
  - Pod anti-affinity for high availability
  - Health checks (liveness, readiness, startup probes)
  - Resource limits and requests for all containers

- **Terraform Infrastructure-as-Code (AWS)**
  - VPC with public/private/database subnets across 3 AZs
  - EKS cluster (v1.28) with managed node groups
  - RDS PostgreSQL 15 (Multi-AZ, automated backups, encryption)
  - ElastiCache Redis 7 cluster (Multi-AZ, automatic failover)
  - S3 buckets with versioning, encryption, and lifecycle policies
  - IAM roles and policies with least privilege
  - IAM Roles for Service Accounts (IRSA) - no long-lived credentials
  - KMS keys for encryption at rest (RDS, S3, EBS, Secrets Manager)
  - AWS Secrets Manager for sensitive data
  - Security groups with minimal access
  - VPC Flow Logs for network monitoring
  - CloudWatch Log Groups with 30-day retention
  - CloudWatch alarms for RDS, ElastiCache, and S3
  - SNS topics for notifications
  - AWS Load Balancer Controller for EKS
  - EBS CSI Driver for persistent volumes

- **Auto-Scaling**
  - Backend HPA: 3-10 replicas based on CPU (70%) and memory (80%)
  - Frontend HPA: 2-5 replicas based on CPU (70%) and memory (80%)
  - EKS Cluster Autoscaler: 2-10 nodes based on demand
  - Intelligent scaling policies with stabilization windows

- **High Availability**
  - Multi-AZ deployment for all critical services
  - Database replication with automatic failover
  - Redis cluster with automatic failover
  - Pod anti-affinity rules to spread across nodes
  - Rolling updates with zero downtime
  - Deletion protection for RDS

- **Security Enhancements**
  - Network isolation with VPC and security groups
  - Encryption at rest for all data stores (KMS)
  - Encryption in transit (TLS/SSL)
  - IAM roles for service accounts (no credentials in pods)
  - Secrets stored in AWS Secrets Manager
  - Non-root containers with security contexts
  - Pod security policies
  - Certificate management with cert-manager
  - Let's Encrypt SSL certificates

- **Monitoring & Observability**
  - CloudWatch integration for AWS resources
  - Prometheus metrics collection
  - Grafana dashboards
  - Centralized logging with CloudWatch Logs
  - Application logs from all pods
  - Audit logs for security events
  - Performance Insights for RDS
  - Enhanced monitoring for RDS and ElastiCache
  - Metric alarms for CPU, memory, storage, and connections

- **Disaster Recovery**
  - Automated daily backups for RDS (30-day retention)
  - Point-in-time recovery enabled
  - S3 versioning for file recovery
  - Cross-region backup replication (configurable)
  - Recovery Time Objective (RTO): < 1 hour
  - Recovery Point Objective (RPO): < 15 minutes

- **Deployment Automation**
  - `setup-aws-infrastructure.sh`: Terraform automation script
  - `deploy-k8s.sh`: Kubernetes deployment script
  - Environment-specific configurations (dev, staging, production)
  - Kustomize overlays for multi-environment support
  - Rolling update strategy with health checks
  - Automated rollback on failure

- **Cost Optimization**
  - S3 Intelligent Tiering and Glacier transitions
  - EBS gp3 volumes with optimized IOPS
  - Right-sized instance types
  - Cluster autoscaler for efficient resource usage
  - Lifecycle policies for old data cleanup
  - Estimated cost: $600-700/month for production

- **Documentation**
  - Comprehensive Phase 4 implementation guide
  - Kubernetes manifests documentation
  - Terraform module documentation
  - Deployment and operations runbooks
  - Troubleshooting guides
  - Security best practices
  - Cost optimization strategies

### Changed
- Updated Dockerfile with multi-stage builds for smaller images
- Enhanced application.yml with production-ready settings
- Configured Spring Boot Actuator for Kubernetes health checks
- Updated logging configuration for CloudWatch compatibility
- Optimized JVM settings for containerized environments

### Infrastructure
- 10 Kubernetes YAML manifests across multiple categories
- 8 Terraform modules for AWS infrastructure
- 100+ lines of Terraform code per module
- 3 automation scripts for deployment
- Support for 3 environments (dev, staging, production)
- Multi-cloud foundation (AWS complete, GCP/Azure ready)

### Database Migrations
- No new migrations (infrastructure only)

## Versioning Strategy

- **v1**: MVP with core storage functionality
- **v2**: Feature maturity (sharing, versioning, search, notifications)
- **v3**: User experience (activity, comments, analytics, galleries, previews)
- **v4**: Cloud-native infrastructure (Kubernetes, Terraform) ✅
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
