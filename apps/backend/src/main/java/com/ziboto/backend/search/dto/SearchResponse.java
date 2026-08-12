package com.ziboto.backend.search.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for file search.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    
    private List<FileResult> results;
    private Long totalResults;
    private Integer totalPages;
    private Integer currentPage;
    private Map<String, Long> facets; // Aggregations (e.g., file types count)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileResult {
        private UUID fileId;
        private String fileName;
        private String originalFileName;
        private String fileExtension;
        private String mimeType;
        private Long fileSize;
        private String formattedFileSize;
        private UUID folderId;
        private String folderPath;
        private LocalDateTime uploadedAt;
        private LocalDateTime lastModified;
        private Float score; // Search relevance score
    }
}
