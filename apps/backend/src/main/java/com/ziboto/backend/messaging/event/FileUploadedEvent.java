package com.ziboto.backend.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a file is successfully uploaded.
 * 
 * <p>Triggers async processing tasks such as:</p>
 * <ul>
 *   <li>Virus scanning</li>
 *   <li>Thumbnail generation</li>
 *   <li>Metadata extraction</li>
 *   <li>Search index updating</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadedEvent {
    
    /**
     * Event unique identifier.
     */
    private UUID eventId;
    
    /**
     * File unique identifier.
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
     * File size in bytes.
     */
    private Long sizeBytes;
    
    /**
     * MIME type.
     */
    private String mimeType;
    
    /**
     * SHA-256 hash for deduplication.
     */
    private String sha256Hash;
    
    /**
     * Folder ID (optional).
     */
    private UUID folderId;
    
    /**
     * Event timestamp.
     */
    private LocalDateTime timestamp;
    
    /**
     * Client IP address (for audit).
     */
    private String ipAddress;
    
    /**
     * User agent (for audit).
     */
    private String userAgent;
}
