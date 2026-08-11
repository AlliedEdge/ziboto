package com.ziboto.backend.share.dto;

import java.time.LocalDateTime;

import com.ziboto.backend.share.enums.SharePermission;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to share a file with another user.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFileShareRequest {
    
    @NotNull(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;
    
    @NotNull(message = "Permission is required")
    private SharePermission permission;
    
    private String message;
    
    private LocalDateTime expiresAt;
}
