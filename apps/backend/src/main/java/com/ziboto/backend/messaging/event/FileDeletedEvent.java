package com.ziboto.backend.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a file is deleted.
 * 
 * <p>Triggers cleanup tasks such as:</p>
 * <ul>
 *   <li>S3 object deletion (if last reference)</li>
 *   <li>Search index removal</li>
 *   <li>Thumbnail deletion</li>
 *   <li>Share link revocation</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDeletedEvent {
    
    /**
     * Event unique identifier.
     */
    private UUID eventId;
    
    /**
     * Deleted file ID.
     */
    private UUID fileId;
    
    /**
     * Owner user ID.
     */
    private Long userId;
    
    /**
     * Original filename.
     */
    private String filename;
    
    /**
     * S3 storage key.
     */
    private String storageKey;
    
    /**
     * SHA-256 hash (for reference counting).
     */
    private String sha256Hash;
    
    /**
     * Event timestamp.
     */
    private LocalDateTime timestamp;
    
    /**
     * Deletion reason (user requested, quota exceeded, etc).
     */
    private String reason;
}
