package com.ziboto.backend.search.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.search.dto.SearchRequest;
import com.ziboto.backend.search.dto.SearchResponse;
import com.ziboto.backend.search.service.FileSearchService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for file search operations.
 * 
 * @author Ziboto Team
 * @since V2
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "File search operations")
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class SearchController {
    
    private final FileSearchService searchService;
    private final UserRepository userRepository;
    
    /**
     * Advanced search with filters and facets.
     * 
     * POST /api/v1/search
     * 
     * @param authentication Current authenticated user
     * @param request Search request with query and filters
     * @return Search results
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Advanced file search", description = "Search files with full-text query and filters")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            Authentication authentication,
            @Valid @RequestBody SearchRequest request) {
        
        Long userId = getUserId(authentication);
        log.info("Search request - user: {}, query: {}", userId, request.getQuery());
        
        SearchResponse response = searchService.search(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Autocomplete suggestions for file names.
     * 
     * GET /api/v1/search/suggestions?q={prefix}
     * 
     * @param authentication Current authenticated user
     * @param prefix Search prefix (min 2 characters)
     * @return List of suggested file names
     */
    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete suggestions", description = "Get file name suggestions based on prefix")
    public ResponseEntity<ApiResponse<List<String>>> autocomplete(
            Authentication authentication,
            @RequestParam("q") String prefix) {
        
        Long userId = getUserId(authentication);
        log.info("Autocomplete request - user: {}, prefix: {}", userId, prefix);
        
        List<String> suggestions = searchService.autocomplete(userId, prefix);
        
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
    
    /**
     * Reindex all files for current user.
     * Useful after bulk operations or index corruption.
     * 
     * POST /api/v1/search/reindex
     * 
     * @param authentication Current authenticated user
     * @return Success message
     */
    @PostMapping("/reindex")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reindex user files", description = "Rebuild search index for current user's files")
    public ResponseEntity<ApiResponse<String>> reindexUserFiles(Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("User reindex request - user: {}", userId);
        
        searchService.reindexUserFiles(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Files reindexed successfully"));
    }
    
    /**
     * Reindex all files in the system (Admin only).
     * 
     * POST /api/v1/search/reindex-all
     * 
     * @param authentication Current authenticated user (must be admin)
     * @return Success message
     */
    @PostMapping("/reindex-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex all files (Admin)", description = "Rebuild search index for all files in the system")
    public ResponseEntity<ApiResponse<String>> reindexAll(Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Full reindex request - admin: {}", userId);
        
        searchService.reindexAll();
        
        return ResponseEntity.ok(ApiResponse.success("All files reindexed successfully"));
    }
    
    /**
     * Extract user ID from authentication.
     * Fetches the user from database using username from JWT token.
     */
    private Long getUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with username: " + username
                ));
        
        return user.getId();
    }
}
