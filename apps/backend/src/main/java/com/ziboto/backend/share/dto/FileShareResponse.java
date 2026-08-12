package com.ziboto.backend.share.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.share.enums.SharePermission;
import com.ziboto.backend.share.enums.ShareStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing file share details.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareResponse {
    
    private UUID id;
    private UUID fileId;
    private String fileName;
    private Long ownerId;
    private String ownerUsername;
    private String ownerFullName;
    private Long sharedWithUserId;
    private String sharedWithUsername;
    private String sharedWithFullName;
    private String sharedWithEmail;
    private SharePermission permission;
    private ShareStatus status;
    private String message;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private boolean expired;
    private boolean active;
}
