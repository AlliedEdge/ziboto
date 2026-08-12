package com.ziboto.backend.share.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.share.enums.ShareLinkPermission;
import com.ziboto.backend.share.enums.ShareLinkStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing share link details.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkResponse {
    
    private UUID id;
    private UUID fileId;
    private String fileName;
    private Long ownerId;
    private String token;
    private String shareUrl;
    private ShareLinkPermission permission;
    private ShareLinkStatus status;
    private boolean passwordProtected;
    private LocalDateTime expiresAt;
    private Integer maxDownloads;
    private Integer downloadCount;
    private LocalDateTime lastAccessedAt;
    private String lastAccessedIp;
    private LocalDateTime createdAt;
    private boolean expired;
    private boolean downloadLimitReached;
    private boolean active;
}
