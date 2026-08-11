package com.ziboto.backend.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OAuth login.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    
    private Long userId;
    private String email;
    private String username;
    
    private Boolean isNewUser; // TRUE if user was just created
    private Boolean accountLinked; // TRUE if OAuth account was linked
}
