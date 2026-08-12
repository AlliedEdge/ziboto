package com.ziboto.backend.search.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for file search.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    
    private String query; // Full-text search query
    
    // Filters
    private List<String> fileExtensions;
    private List<String> mimeTypes;
    private String folderId;
    
    // Size range
    private Long minSize;
    private Long maxSize;
    
    // Date range
    private LocalDateTime uploadedAfter;
    private LocalDateTime uploadedBefore;
    
    // Pagination
    private Integer page;
    private Integer size;
    
    // Sorting
    private String sortBy; // fileName, fileSize, uploadedAt
    private String sortDirection; // ASC, DESC
}
