package com.ziboto.backend.version.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new file version.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionRequest {
    
    /**
     * Optional description of what changed.
     */
    @Size(max = 1000, message = "Change description cannot exceed 1000 characters")
    private String changeDescription;
    
    /**
     * Optional version tag (e.g., "v1.0", "final", "draft").
     */
    @Size(max = 100, message = "Version tag cannot exceed 100 characters")
    private String versionTag;
}
