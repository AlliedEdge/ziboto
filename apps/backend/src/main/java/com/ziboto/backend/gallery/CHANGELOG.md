# Gallery Module Changelog

## [1.0.0] - 2026-08-12

### Added
- Gallery entity with slug-based URLs
- GalleryFile entity for junction table
- GalleryRepository with slug lookup
- GalleryFileRepository with ordering support
- GalleryService with full CRUD operations
- GalleryController with 9 REST endpoints:
  - POST /api/v1/galleries - Create gallery
  - GET /api/v1/galleries - List user galleries
  - GET /api/v1/galleries/{id} - Get gallery by ID
  - GET /api/v1/galleries/public/{slug} - Get public gallery
  - PUT /api/v1/galleries/{id} - Update gallery
  - DELETE /api/v1/galleries/{id} - Delete gallery
  - POST /api/v1/galleries/{id}/files - Add file to gallery
  - DELETE /api/v1/galleries/{id}/files/{fileId} - Remove file
  - PUT /api/v1/galleries/{id}/files/reorder - Reorder files
- GalleryRequest, GalleryResponse, GalleryDetailResponse DTOs
- AddFileRequest DTO for adding files
- Database migration V21__public_galleries.sql with:
  - galleries table
  - gallery_files junction table
  - 7 indexes for query optimization
  - 4 database functions

### Features
- **Multiple Themes**
  - default, dark, light, minimal, vibrant
  - Customizable gallery appearance

- **Multiple Layouts**
  - grid - Traditional grid layout
  - masonry - Pinterest-style layout
  - slideshow - Full-screen slideshow
  - list - List view with details

- **Slug-Based URLs**
  - SEO-friendly URLs (/gallery/my-vacation-photos)
  - Automatic slug generation from title
  - Unique slug enforcement with counters

- **Access Control**
  - Public/private galleries
  - Password protection support
  - BCrypt password hashing

- **File Management**
  - Add/remove files from gallery
  - Custom captions per file
  - Drag-and-drop reordering
  - Display order management

- **Analytics**
  - View count tracking
  - File count statistics
  - Total gallery size calculation

### Database Schema
```sql
CREATE TABLE galleries (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(255) UNIQUE NOT NULL,
    is_public BOOLEAN DEFAULT TRUE,
    password_protected BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),
    theme VARCHAR(50) DEFAULT 'default',
    layout VARCHAR(50) DEFAULT 'grid',
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE gallery_files (
    id UUID PRIMARY KEY,
    gallery_id UUID NOT NULL,
    file_id UUID NOT NULL,
    display_order INT DEFAULT 0,
    caption TEXT,
    added_at TIMESTAMP NOT NULL
);
```

### Performance
- Indexed on slug for fast public access
- Indexed on user_id for owner queries
- Indexed on display_order for sorting
- Efficient file loading with joins

### Security
- Authentication required for creation/editing
- Ownership validation on all operations
- Password verification for protected galleries
- Public galleries accessible without auth

### Integration
- FileMetadata integration for file details
- Activity logging for gallery operations
- User profile for gallery ownership
- S3 URLs for file access

### Database Functions
- `get_gallery_with_files(gallery_id)` - Gallery with statistics
- `get_user_galleries(user_id, limit, offset)` - Paginated user galleries
- `increment_gallery_views(gallery_id)` - View counter
- `generate_gallery_slug(title)` - Unique slug generation

### Future Enhancements
- Gallery templates
- Batch file operations
- Gallery sharing/collaboration
- Gallery analytics dashboard
- Export gallery as zip
- Gallery embedding (iframe)
