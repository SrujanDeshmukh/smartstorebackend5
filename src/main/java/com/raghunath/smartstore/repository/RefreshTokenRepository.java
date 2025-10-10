package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    // Delete all tokens by email (logout from all devices)
    void deleteByEmail(String email);

    // Delete tokens by email and user type (logout specific user type)
    void deleteByEmailAndUserType(String email, String userType);

    // Find token by token string
    Optional<RefreshToken> findByToken(String token);

    // Find token by email and user type
    Optional<RefreshToken> findByEmailAndUserType(String email, String userType);

    // Count active tokens by user type (for monitoring)
    long countByUserType(String userType);

    // Check if token exists for email and user type
    boolean existsByEmailAndUserType(String email, String userType);
}
