package com.ziboto.backend.search.repository;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.search.document.FileDocument;

/**
 * Elasticsearch repository for file search.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public interface FileSearchRepository extends ElasticsearchRepository<FileDocument, String> {
    
    /**
     * Find files by user ID.
     */
    Page<FileDocument> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Full-text search in file name.
     */
    Page<FileDocument> findByUserIdAndFileNameContaining(Long userId, String fileName, Pageable pageable);
    
    /**
     * Find by file extension.
     */
    Page<FileDocument> findByUserIdAndFileExtension(Long userId, String extension, Pageable pageable);
    
    /**
     * Find by mime type.
     */
    Page<FileDocument> findByUserIdAndMimeType(Long userId, String mimeType, Pageable pageable);
    
    /**
     * Find by folder.
     */
    Page<FileDocument> findByUserIdAndFolderId(Long userId, String folderId, Pageable pageable);
    
    /**
     * Advanced search with custom query.
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"userId\": \"?0\"}}, {\"multi_match\": {\"query\": \"?1\", \"fields\": [\"fileName^2\", \"originalFileName\", \"tags\"]}}]}}")
    Page<FileDocument> searchByUserIdAndQuery(Long userId, String query, Pageable pageable);
}
