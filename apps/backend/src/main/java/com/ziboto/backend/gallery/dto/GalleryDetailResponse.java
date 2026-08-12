package com.ziboto.backend.gallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for detailed gallery responses with files.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDetailResponse {
    
    private UUID id;
    private Long userId;
    private String username;
    private String title;
    private String description;
    private String slug;
    private Boolean isPublic;
    private Boolean passwordProtected;
    private String theme;
    private String layout;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String publicUrl;
    private List<GalleryFileResponse> files;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GalleryFileResponse {
        private UUID galleryFileId;
        private UUID fileId;
        private String filename;
        private String contentType;
        private Long fileSize;
        private String s3Key;
        private String thumbnailUrl;
        private String previewUrl;
        private Integer displayOrder;
        private String caption;
        private LocalDateTime addedAt;
    }
}
