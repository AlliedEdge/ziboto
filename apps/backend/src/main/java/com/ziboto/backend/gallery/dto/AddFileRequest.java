package com.ziboto.backend.gallery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for adding a file to a gallery.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFileRequest {
    
    @NotNull(message = "File ID is required")
    private UUID fileId;
    
    @Size(max = 1000, message = "Caption must not exceed 1000 characters")
    private String caption;
    
    private Integer displayOrder;
}
