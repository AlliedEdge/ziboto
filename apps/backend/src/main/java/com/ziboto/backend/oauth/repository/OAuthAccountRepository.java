package com.ziboto.backend.oauth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.oauth.entity.OAuthAccount;
import com.ziboto.backend.oauth.enums.OAuthProvider;

/**
 * Repository for OAuthAccount entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    
    /**
     * Find OAuth account by user and provider.
     */
    Optional<OAuthAccount> findByUserIdAndProvider(Long userId, OAuthProvider provider);
    
    /**
     * Find OAuth account by provider and provider user ID.
     */
    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    
    /**
     * Find all OAuth accounts for a user.
     */
    List<OAuthAccount> findByUserId(Long userId);
    
    /**
     * Find active OAuth accounts for a user.
     */
    @Query("SELECT oa FROM OAuthAccount oa WHERE oa.userId = :userId AND oa.isActive = true")
    List<OAuthAccount> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * Check if user has linked OAuth provider.
     */
    boolean existsByUserIdAndProvider(Long userId, OAuthProvider provider);
    
    /**
     * Check if provider user ID is already linked.
     */
    boolean existsByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    
    /**
     * Find OAuth account by email and provider.
     */
    Optional<OAuthAccount> findByEmailAndProvider(String email, OAuthProvider provider);
}
