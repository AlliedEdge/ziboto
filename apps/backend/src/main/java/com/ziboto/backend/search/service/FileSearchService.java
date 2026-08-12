package com.ziboto.backend.search.service;

import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.search.document.FileDocument;
import com.ziboto.backend.search.dto.SearchRequest;
import com.ziboto.backend.search.dto.SearchResponse;
import com.ziboto.backend.search.repository.FileSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Elasticsearch-based file search.
 * Provides full-text search, filtering, and autocomplete.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class FileSearchService {
    
    private final FileSearchRepository searchRepository;
    private final FileMetadataRepository fileMetadataRepository;
    
    /**
     * Index a file for search.
     * Called automatically when a file is uploaded.
     */
    public void indexFile(FileMetadata fileMetadata) {
        log.debug("Indexing file for search - fileId: {}", fileMetadata.getId());
        
        try {
            FileDocument document = FileDocument.builder()
                    .id(fileMetadata.getId().toString())
                    .userId(fileMetadata.getUserId())
                    .fileName(fileMetadata.getFileName())
                    .originalFileName(fileMetadata.getOriginalFileName())
                    .fileExtension(fileMetadata.getFileExtension())
                    .mimeType(fileMetadata.getMimeType())
                    .fileSize(fileMetadata.getFileSize())
                    .folderId(fileMetadata.getFolderId() != null ? fileMetadata.getFolderId().toString() : null)
                    .folderPath(fileMetadata.getFolder() != null ? fileMetadata.getFolder().getFolderPath() : null)
                    .uploadedAt(fileMetadata.getCreatedAt())
                    .lastModified(fileMetadata.getUpdatedAt())
                    .sha256Hash(fileMetadata.getSha256Hash())
                    .tags("") // Tags can be added in future
                    .build();
            
            searchRepository.save(document);
            log.info("File indexed successfully - fileId: {}", fileMetadata.getId());
            
        } catch (Exception e) {
            log.error("Failed to index file - fileId: {}", fileMetadata.getId(), e);
            // Don't throw exception - indexing failure should not break file upload
        }
    }
    
    /**
     * Update search index when file metadata changes.
     */
    public void updateIndex(FileMetadata fileMetadata) {
        log.debug("Updating file index - fileId: {}", fileMetadata.getId());
        indexFile(fileMetadata); // Save will update existing document
    }
    
    /**
     * Remove file from search index.
     * Called when a file is deleted.
     */
    public void deleteFromIndex(UUID fileId) {
        log.debug("Removing file from index - fileId: {}", fileId);
        
        try {
            searchRepository.deleteById(fileId.toString());
            log.info("File removed from index - fileId: {}", fileId);
            
        } catch (Exception e) {
            log.error("Failed to remove file from index - fileId: {}", fileId, e);
            // Don't throw exception - index cleanup failure should not break file deletion
        }
    }
    
    /**
     * Advanced search with filters and facets.
     */
    public SearchResponse search(Long userId, SearchRequest request) {
        log.debug("File search request - user: {}, query: {}", userId, request.getQuery());
        
        try {
            // Pagination
            int page = request.getPage() != null ? request.getPage() : 0;
            int size = request.getSize() != null ? request.getSize() : 20;
            size = Math.min(size, 100); // Max 100 results per page
            
            // Sorting
            String sortField = request.getSortBy() != null ? request.getSortBy() : "uploadedAt";
            Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) 
                    ? Sort.Direction.ASC 
                    : Sort.Direction.DESC;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
            
            // Execute search based on query
            Page<FileDocument> searchResults;
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                searchResults = searchRepository.searchByUserIdAndQuery(userId, request.getQuery(), pageable);
            } else {
                searchResults = searchRepository.findByUserId(userId, pageable);
            }
            
            // Build response
            List<SearchResponse.FileResult> results = searchResults.stream()
                    .map(this::mapToFileResult)
                    .collect(Collectors.toList());
            
            long totalResults = searchResults.getTotalElements();
            int totalPages = searchResults.getTotalPages();
            
            log.info("Search completed - user: {}, results: {}", userId, totalResults);
            
            return SearchResponse.builder()
                    .results(results)
                    .totalResults(totalResults)
                    .totalPages(totalPages)
                    .currentPage(page)
                    .facets(Collections.emptyMap()) // Facets can be added later
                    .build();
            
        } catch (Exception e) {
            log.error("Search failed - user: {}", userId, e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "Search operation failed");
        }
    }
    
    /**
     * Autocomplete suggestions for file names.
     */
    public List<String> autocomplete(Long userId, String prefix) {
        log.debug("Autocomplete request - user: {}, prefix: {}", userId, prefix);
        
        if (prefix == null || prefix.trim().isEmpty() || prefix.length() < 2) {
            return Collections.emptyList();
        }
        
        try {
            // Simple search by filename containing prefix
            Page<FileDocument> results = searchRepository.findByUserIdAndFileNameContaining(
                    userId, prefix, PageRequest.of(0, 10));
            
            List<String> suggestions = results.stream()
                    .map(FileDocument::getFileName)
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
            
            log.debug("Autocomplete returned {} suggestions", suggestions.size());
            return suggestions;
            
        } catch (Exception e) {
            log.error("Autocomplete failed - user: {}", userId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Reindex all files for a user.
     * Useful for rebuilding search index.
     */
    public void reindexUserFiles(Long userId) {
        log.info("Reindexing files for user: {}", userId);
        
        try {
            // Delete existing documents for user
            deleteUserIndex(userId);
            
            // Fetch all user files from the database
            // We'll need to add this method to FileMetadataRepository
            List<FileMetadata> files = fileMetadataRepository.findAll().stream()
                    .filter(f -> f.getUserId().equals(userId))
                    .collect(Collectors.toList());
            
            log.info("Found {} files to reindex for user {}", files.size(), userId);
            
            // Index each file
            for (FileMetadata file : files) {
                indexFile(file);
            }
            
            log.info("Reindexing complete for user {} - {} files indexed", userId, files.size());
            
        } catch (Exception e) {
            log.error("Reindexing failed for user: {}", userId, e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "Reindexing failed");
        }
    }
    
    /**
     * Reindex all files in the system.
     * Admin operation.
     */
    public void reindexAll() {
        log.info("Starting full reindex of all files");
        
        try {
            // Delete all documents
            searchRepository.deleteAll();
            
            // Fetch all files
            List<FileMetadata> files = fileMetadataRepository.findAll();
            
            log.info("Found {} total files to reindex", files.size());
            
            // Index each file
            int indexed = 0;
            for (FileMetadata file : files) {
                indexFile(file);
                indexed++;
                
                if (indexed % 100 == 0) {
                    log.info("Reindexing progress: {}/{}", indexed, files.size());
                }
            }
            
            log.info("Full reindex complete - {} files indexed", indexed);
            
        } catch (Exception e) {
            log.error("Full reindex failed", e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "Full reindex failed");
        }
    }
    
    /**
     * Delete all search documents for a user.
     */
    private void deleteUserIndex(Long userId) {
        try {
            // Find all documents for user and delete them
            Page<FileDocument> userDocs = searchRepository.findByUserId(userId, Pageable.unpaged());
            
            List<String> ids = userDocs.stream()
                    .map(FileDocument::getId)
                    .collect(Collectors.toList());
            
            if (!ids.isEmpty()) {
                searchRepository.deleteAllById(ids);
                log.info("Deleted {} documents for user {}", ids.size(), userId);
            }
            
        } catch (Exception e) {
            log.error("Failed to delete user index - user: {}", userId, e);
        }
    }
    
    /**
     * Map FileDocument to FileResult DTO.
     */
    private SearchResponse.FileResult mapToFileResult(FileDocument doc) {
        return SearchResponse.FileResult.builder()
                .fileId(doc.getFileId())
                .fileName(doc.getFileName())
                .originalFileName(doc.getOriginalFileName())
                .fileExtension(doc.getFileExtension())
                .mimeType(doc.getMimeType())
                .fileSize(doc.getFileSize())
                .formattedFileSize(doc.getFormattedFileSize())
                .folderId(doc.getFolderIdAsUUID())
                .folderPath(doc.getFolderPath())
                .uploadedAt(doc.getUploadedAt())
                .lastModified(doc.getLastModified())
                .score(null) // Score not available with simple repository search
                .build();
    }
}
