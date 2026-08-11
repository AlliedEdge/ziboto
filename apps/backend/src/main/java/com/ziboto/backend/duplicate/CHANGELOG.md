# Changelog - Duplicate Detection Module

All notable changes to the duplicate detection module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- SHA-256 hash-based duplicate detection
- Duplicate file checking before upload
- DeduplicationService for hash comparison
- Database index on sha256_hash column
- Automatic duplicate reference linking
- Space-saving through deduplication

### Features
- Calculate SHA-256 hash for uploaded files
- Check if file with same hash exists
- Link duplicate files to same S3 object
- Track duplicate count per unique file
- Report storage savings from deduplication

### Performance
- Indexed hash lookups for fast duplicate detection
- O(1) hash comparison
- Reduced S3 storage through file reuse

### Technical
- SHA-256 algorithm via Java MessageDigest
- Hash stored in file_metadata table
- Deduplication logic in file upload flow
