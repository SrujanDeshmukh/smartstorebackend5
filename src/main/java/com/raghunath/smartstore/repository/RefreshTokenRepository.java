package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    void deleteByEmail(String email);

    Optional<RefreshToken> findByToken(String token);
}
