package com.ziboto.backend.gallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for gallery responses.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryResponse {
    
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
    private Long fileCount;
    private Long totalSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String publicUrl;
}
