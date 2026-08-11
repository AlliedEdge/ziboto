package com.ziboto.backend.gallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for gallery creation/update requests.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    private Boolean isPublic;
    
    private Boolean passwordProtected;
    
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    private String password;
    
    @Pattern(regexp = "default|dark|light|minimal|vibrant", message = "Invalid theme")
    private String theme;
    
    @Pattern(regexp = "grid|masonry|slideshow|list", message = "Invalid layout")
    private String layout;
}
