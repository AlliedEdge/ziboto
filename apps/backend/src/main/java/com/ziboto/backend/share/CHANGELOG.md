# Changelog - Share Module

All notable changes to the file sharing module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- File and folder sharing system
- Public share links
- Password-protected shares
- Expiring share links
- Share permissions

### Features
- Generate shareable links
- Share individual files
- Share entire folders
- Password protection option
- Link expiration (custom or never)
- View count tracking
- Download tracking

### Share Link Structure
- Unique share token (UUID)
- Shareable URL format
- Public access (no auth required)
- Password prompt if protected

### Security
- BCrypt password hashing for share links
- Token-based access
- Expiration validation
- Owner verification
- Rate limiting on share access

### Permissions
- VIEW_ONLY: Can view and download
- EDIT: Can modify (future)
- COMMENT: Can add comments (V3 integration)

### Analytics
- View count per share
- Download count per share
- Last accessed timestamp
- Access log (future)

### API Endpoints
- Create share link
- Get share details
- Access shared file/folder
- Update share settings
- Delete/revoke share
- List user's shares

### Database
- share_links table
- Indexed on token
- Indexed on expiration
- Foreign key to file/folder
