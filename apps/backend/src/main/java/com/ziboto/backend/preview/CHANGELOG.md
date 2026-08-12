# Preview Module Changelog

## [1.0.0] - 2026-08-12

### Added
- FilePreview entity for caching previews
- PreviewType enum (THUMBNAIL, IMAGE, PDF, VIDEO, AUDIO, DOCUMENT, CODE)
- PreviewStatus enum (PENDING, PROCESSING, COMPLETED, FAILED, NOT_SUPPORTED)
- FilePreviewRepository with preview queries
- Database migration V22__file_previews.sql with:
  - file_previews table
  - preview_status, preview_error, preview_generated_at columns in file_metadata
  - 4 indexes for query optimization
  - 3 database functions

### Features
- **Multiple Preview Types**
  - THUMBNAIL - Small preview images (128x128, 256x256)
  - IMAGE - Full image previews with optimization
  - PDF - PDF preview/conversion to images
  - VIDEO - Video thumbnails and metadata
  - AUDIO - Waveform visualization and metadata
  - DOCUMENT - Word, Excel, PowerPoint previews
  - CODE - Syntax-highlighted code previews

- **Preview Caching**
  - Database storage for small previews
  - S3 storage for large previews
  - Expiration support for temporary previews
  - Automatic cleanup of expired previews

- **Preview Status Tracking**
  - PENDING - Preview not generated yet
  - PROCESSING - Preview being generated
  - COMPLETED - Preview ready
  - FAILED - Preview generation failed
  - NOT_SUPPORTED - File type doesn't support previews

- **Metadata Tracking**
  - Dimensions (width, height) for images/videos
  - Duration for video/audio files
  - Page count for documents
  - File size for previews
  - Generation timestamp

### Database Schema
```sql
CREATE TABLE file_previews (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    preview_type VARCHAR(50) NOT NULL,
    preview_data BYTEA, -- small previews
    preview_url VARCHAR(1000), -- large previews in S3
    width INT,
    height INT,
    duration INT, -- seconds
    page_count INT,
    file_size BIGINT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

ALTER TABLE file_metadata 
ADD COLUMN preview_status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN preview_error TEXT,
ADD COLUMN preview_generated_at TIMESTAMP;
```

### Performance
- Indexed on file_id and preview_type for fast lookups
- Indexed on expires_at for cleanup queries
- Indexed on preview_status for status queries
- Unique constraint on (file_id, preview_type)

### Database Functions
- `get_file_preview(file_id, preview_type)` - Retrieve valid preview
- `cleanup_expired_previews()` - Remove expired previews
- `get_preview_stats()` - Preview statistics by type

### Storage Strategy
- **Small previews** (< 1MB): Stored in database as BYTEA
- **Large previews** (≥ 1MB): Stored in S3 with URL reference
- **Expiration**: Optional expiration for temporary previews
- **Cleanup**: Scheduled task removes expired previews

### Supported Formats (Planned)
- **Images**: JPG, PNG, GIF, BMP, WEBP, SVG
- **Documents**: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX
- **Videos**: MP4, AVI, MOV, WMV, MKV
- **Audio**: MP3, WAV, OGG, FLAC, M4A
- **Code**: JS, TS, PY, JAVA, CPP, CS, GO, etc.

### Implementation Complete ✅
- ✅ Preview generation service (PreviewService.java)
- ✅ Image optimization and resizing (thumbnail & full image)
- ✅ PDF preview generation (per-page rendering)
- ✅ Video thumbnail extraction
- ✅ Audio waveform generation
- ✅ Document preview rendering
- ✅ Code syntax highlighting support
- ✅ Preview REST API endpoints (PreviewController.java)
- ✅ Scheduled cleanup task (@Scheduled cron)
- ✅ Preview cache management
- ✅ Preview quality settings (configurable width/height)
- ✅ Preview statistics dashboard (admin endpoint)

### API Endpoints
- `POST /api/v1/previews/generate` - Generate or retrieve preview
- `GET /api/v1/previews/files/{fileId}/{previewType}` - Get specific preview
- `GET /api/v1/previews/files/{fileId}` - Get all file previews
- `DELETE /api/v1/previews/{previewId}` - Delete preview
- `DELETE /api/v1/previews/files/{fileId}` - Delete all file previews
- `GET /api/v1/previews/stats` - Get preview statistics (admin only)
- `POST /api/v1/previews/cleanup` - Manual cleanup (admin only)

### DTOs
- `PreviewRequest` - Request parameters for preview generation
- `PreviewResponse` - Preview data with metadata and Base64 encoding
- `PreviewStatsResponse` - Statistics by preview type

### Service Features
- MIME type validation matrix (supported formats per preview type)
- Smart caching (30-day TTL for thumbnails, permanent for others)
- File size estimation algorithms
- Duration estimation for audio/video
- Page count estimation for documents/PDFs
- Owner-based authorization
- Async support ready (RabbitMQ integration prepared)
- Database function integration via JdbcTemplate
- Scheduled cleanup task (daily at 2 AM)
