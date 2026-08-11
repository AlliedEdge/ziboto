package com.ziboto.backend.activity.enums;

/**
 * Enum representing different types of user activities.
 * 
 * @author Ziboto Team
 * @since V3
 */
public enum ActivityType {
    // File Activities
    FILE_UPLOADED,
    FILE_DOWNLOADED,
    FILE_DELETED,
    FILE_UPDATED,
    FILE_SHARED,
    FILE_UNSHARED,
    FILE_RESTORED,
    
    // Folder Activities
    FOLDER_CREATED,
    FOLDER_DELETED,
    FOLDER_RENAMED,
    FOLDER_MOVED,
    
    // User Activities
    USER_LOGIN,
    USER_LOGOUT,
    USER_REGISTERED,
    USER_UPDATED,
    
    // Share Activities
    SHARE_ACCEPTED,
    SHARE_DECLINED,
    
    // Version Activities
    VERSION_CREATED,
    VERSION_RESTORED,
    
    // Comment Activities
    COMMENT_ADDED,
    COMMENT_DELETED,
    
    // Duplicate Activities
    DUPLICATE_DETECTED,
    DUPLICATE_REMOVED,
    
    // Gallery Activities
    GALLERY_CREATED,
    GALLERY_DELETED,
    GALLERY_UPDATED
}
