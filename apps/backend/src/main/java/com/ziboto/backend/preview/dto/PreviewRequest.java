package com.ziboto.backend.preview.dto;

import com.ziboto.backend.preview.enums.PreviewType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for preview generation requests.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewRequest {
    
    @NotNull(message = "File ID is required")
    private UUID fileId;
    
    @NotNull(message = "Preview type is required")
    private PreviewType previewType;
    
    // Optional parameters for specific preview types
    private Integer width;
    private Integer height;
    private Integer quality; // 1-100 for images
    private Integer page; // For document previews
    private Boolean async; // Generate asynchronously via RabbitMQ
}
