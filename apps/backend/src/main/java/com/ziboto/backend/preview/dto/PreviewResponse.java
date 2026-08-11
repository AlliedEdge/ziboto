package com.ziboto.backend.preview.dto;

import com.ziboto.backend.preview.enums.PreviewStatus;
import com.ziboto.backend.preview.enums.PreviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for preview responses.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponse {
    
    private UUID id;
    private UUID fileId;
    private PreviewType previewType;
    private PreviewStatus status;
    private String previewUrl;
    private String previewData; // Base64 for inline previews
    private Integer width;
    private Integer height;
    private Integer duration;
    private Integer pageCount;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String errorMessage;
}
