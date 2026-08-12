# Changelog - Search Module

All notable changes to the search module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- Advanced search functionality
- Multi-criteria search
- Full-text search with Elasticsearch (disabled)
- Database search as primary

### Search Criteria
- **Filename**: Partial match (ILIKE)
- **File type**: MIME type filtering
- **File extension**: Extension-based search
- **Size range**: Min/max file size
- **Date range**: Created/updated date filtering
- **Owner**: Search by user
- **Folder**: Search within specific folder

### Features
- Pagination support
- Sorting (name, size, date)
- Case-insensitive search
- Wildcard search
- Combined criteria (AND logic)

### Performance
- Database indexes on searchable columns
- Query optimization
- Limit result sets
- Efficient pagination

### Database Search
- PostgreSQL ILIKE for text matching
- Index on file_name column
- Index on mime_type column
- Composite indexes for common queries

### Elasticsearch (Disabled)
- Full-text search capability
- Faceted search
- Relevance scoring
- Typo tolerance

**Note**: Elasticsearch was disabled per user request. All search currently uses PostgreSQL.

### Future Enhancements (V5)
- AI-powered smart search
- Natural language queries
- Image similarity search
- Content-based search
- Search suggestions
- Search history
