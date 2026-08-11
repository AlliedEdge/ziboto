package com.ziboto.backend.preview.enums;

/**
 * Enum for preview generation status.
 * 
 * @author Ziboto Team
 * @since V3
 */
public enum PreviewStatus {
    PENDING,        // Preview generation not started
    PROCESSING,     // Preview is being generated
    COMPLETED,      // Preview generated successfully
    FAILED,         // Preview generation failed
    NOT_SUPPORTED   // File type doesn't support previews
}
