package com.ziboto.backend.share.enums;

/**
 * Permission levels for public share links.
 * 
 * <p>More restrictive than direct shares since anyone with
 * the link can access.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
public enum ShareLinkPermission {
    /**
     * View file metadata only.
     * Cannot download content.
     */
    VIEW,
    
    /**
     * View metadata and download file content.
     */
    DOWNLOAD;
    
    /**
     * Check if this permission allows downloading.
     */
    public boolean canDownload() {
        return this == DOWNLOAD;
    }
}
