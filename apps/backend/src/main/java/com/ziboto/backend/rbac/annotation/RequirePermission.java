package com.ziboto.backend.rbac.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level permission checks.
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}RequirePermission("files:delete")
 * public void deleteFile(UUID fileId) {
 *     // Only users with "files:delete" permission can execute this
 * }
 * 
 * {@literal @}RequirePermission(value = {"files:create", "folders:create"}, requireAll = true)
 * public void createFileInFolder(MultipartFile file, UUID folderId) {
 *     // Requires both permissions
 * }
 * </pre>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * Required permission(s).
     */
    String[] value();
    
    /**
     * If true, user must have ALL permissions.
     * If false, user must have ANY permission.
     * Default: true
     */
    boolean requireAll() default true;
    
    /**
     * Custom error message when permission is denied.
     */
    String message() default "Access denied: insufficient permissions";
}
