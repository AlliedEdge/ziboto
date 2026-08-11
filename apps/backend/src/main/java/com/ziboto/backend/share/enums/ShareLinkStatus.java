package com.ziboto.backend.share.enums;

/**
 * Status of a share link.
 * 
 * @author Ziboto Team
 * @since V2
 */
public enum ShareLinkStatus {
    /**
     * Link is active and can be accessed.
     */
    ACTIVE,
    
    /**
     * Link disabled by owner (can be re-enabled).
     */
    DISABLED,
    
    /**
     * Link expired (reached expiration date or download limit).
     */
    EXPIRED;
    
    /**
     * Check if link is active.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}
