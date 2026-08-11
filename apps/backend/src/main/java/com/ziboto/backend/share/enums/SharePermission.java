package com.ziboto.backend.share.enums;

/**
 * Permission levels for direct file/folder shares.
 * 
 * @author Ziboto Team
 * @since V2
 */
public enum SharePermission {
    /**
     * View metadata only (filename, size, type, etc).
     * Cannot download or modify.
     */
    VIEW,
    
    /**
     * View and modify file properties/metadata.
     * Cannot download content.
     */
    EDIT,
    
    /**
     * View and download file content.
     * Cannot modify.
     */
    DOWNLOAD,
    
    /**
     * Full permissions: view, download, edit, delete.
     */
    FULL;
    
    /**
     * Check if this permission allows downloading.
     */
    public boolean canDownload() {
        return this == DOWNLOAD || this == FULL;
    }
    
    /**
     * Check if this permission allows editing.
     */
    public boolean canEdit() {
        return this == EDIT || this == FULL;
    }
    
    /**
     * Check if this permission allows deletion.
     */
    public boolean canDelete() {
        return this == FULL;
    }
}
