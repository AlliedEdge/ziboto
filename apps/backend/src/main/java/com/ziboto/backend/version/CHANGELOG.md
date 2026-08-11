# Changelog - Version Control Module

All notable changes to the file versioning module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- File versioning system
- FileVersion entity
- Version history tracking
- Version restore functionality
- Version comparison

### Features
- Automatic version creation on file update
- Version number increment
- Version metadata (size, upload time, user)
- S3 key tracking per version
- Previous version restoration
- Version deletion

### Database
- file_versions table
- Foreign key to file_metadata
- Version number (sequential)
- S3 key for each version
- Created timestamp
- User who created version

### API (Planned)
- Get file version history
- Restore specific version
- Delete old versions
- Compare versions
- Download specific version

### Storage
- Each version stored separately in S3
- Unique S3 key per version
- Metadata links to all versions

### Future Enhancements (V5)
- Version diff viewer
- Automatic version cleanup (keep last N)
- Version compression
- Version branching
- Conflict resolution
- Collaborative versioning
