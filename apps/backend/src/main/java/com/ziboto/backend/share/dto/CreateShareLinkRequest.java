package com.ziboto.backend.share.dto;

import java.time.LocalDateTime;

import com.ziboto.backend.share.enums.ShareLinkPermission;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a public share link.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShareLinkRequest {
    
    @NotNull(message = "Permission is required")
    private ShareLinkPermission permission;
    
    private String password;
    
    private LocalDateTime expiresAt;
    
    @Min(value = 1, message = "Max downloads must be at least 1")
    private Integer maxDownloads;
}
