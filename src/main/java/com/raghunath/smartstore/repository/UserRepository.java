package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Optional<User> findByLocation(String location);

    // New methods for enhanced functionality
    long countByIsActiveTrue();
    long countByFailedLoginAttemptsGreaterThanEqual(int attempts);
}

