package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    // ================================
    // BASIC FIND OPERATIONS
    // ================================

    /**
     * Find refresh token by token string (unique lookup)
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens by email (for logout all devices)
     */
    List<RefreshToken> findByEmail(String email);

    /**
     * Find all refresh tokens by email and user type (multiple tokens possible)
     * ✅ FIXED: Returns List instead of Optional
     */
    List<RefreshToken> findByEmailAndUserType(String email, String userType);

    /**
     * Find refresh tokens by user type only
     */
    List<RefreshToken> findByUserType(String userType);

    /**
     * Find refresh tokens created after specific date
     */
    List<RefreshToken> findByCreatedAtAfter(LocalDateTime date);

    // ================================
    // DELETE OPERATIONS
    // ================================

    /**
     * Delete all refresh tokens by email (logout from all devices)
     * Returns number of deleted records
     */
    @Transactional
    long deleteByEmail(String email);

    /**
     * Delete refresh tokens by email and user type (logout specific user type)
     * Returns number of deleted records
     */
    @Transactional
    long deleteByEmailAndUserType(String email, String userType);

    /**
     * Delete all refresh tokens by user type
     * Returns number of deleted records
     */
    @Transactional
    long deleteByUserType(String userType);

    /**
     * Delete refresh tokens created before specific date (cleanup)
     * Returns number of deleted records
     */
    @Transactional
    long deleteByCreatedAtBefore(LocalDateTime date);

    // ================================
    // EXISTENCE CHECKS
    // ================================

    /**
     * Check if any refresh token exists for email and user type
     */
    boolean existsByEmailAndUserType(String email, String userType);

    /**
     * Check if specific token exists
     */
    boolean existsByToken(String token);

    /**
     * Check if any token exists for email
     */
    boolean existsByEmail(String email);

    // ================================
    // COUNT OPERATIONS
    // ================================

    /**
     * Count active tokens by user type (for monitoring)
     */
    long countByUserType(String userType);

    /**
     * Count tokens by email (how many devices/sessions)
     */
    long countByEmail(String email);

    /**
     * Count tokens created after specific date
     */
    long countByCreatedAtAfter(LocalDateTime date);

    // ================================
    // CUSTOM QUERIES (Advanced)
    // ================================

    /**
     * Find tokens that are potentially expired (for cleanup)
     * Note: This assumes you have createdAt field and token validity period
     */
    @Query("{ 'createdAt': { $lt: ?0 } }")
    List<RefreshToken> findExpiredTokens(LocalDateTime cutoffDate);

    /**
     * Find top active users by token count
     */
    @Query(value = "{ }", sort = "{ 'email': 1 }")
    List<RefreshToken> findAllOrderByEmail();

    /**
     * Count total active sessions (all tokens)
     */
    @Query(value = "{ }", count = true)
    long countTotalActiveSessions();

    /**
     * Find tokens for multiple user types
     */
    List<RefreshToken> findByUserTypeIn(List<String> userTypes);

    /**
     * Find tokens by email pattern (partial email matching)
     */
    @Query("{ 'email': { $regex: ?0, $options: 'i' } }")
    List<RefreshToken> findByEmailContainingIgnoreCase(String emailPattern);

    // ================================
    // BATCH OPERATIONS
    // ================================

    /**
     * Delete tokens for multiple emails (batch logout)
     */
    @Transactional
    long deleteByEmailIn(List<String> emails);

    /**
     * Delete tokens for multiple user types
     */
    @Transactional
    long deleteByUserTypeIn(List<String> userTypes);

    /**
     * Find tokens for multiple emails
     */
    List<RefreshToken> findByEmailIn(List<String> emails);
}
