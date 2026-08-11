package com.ziboto.backend.share.enums;

/**
 * Status of a file/folder share.
 * 
 * @author Ziboto Team
 * @since V2
 */
public enum ShareStatus {
    /**
     * Share invitation sent, awaiting recipient's response.
     */
    PENDING,
    
    /**
     * Recipient accepted the share, currently active.
     */
    ACCEPTED,
    
    /**
     * Recipient declined the share.
     */
    DECLINED,
    
    /**
     * Owner revoked the share (cancelled).
     */
    REVOKED;
    
    /**
     * Check if share is active.
     */
    public boolean isActive() {
        return this == ACCEPTED;
    }
}
