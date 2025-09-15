package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.OtpRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends MongoRepository<OtpRecord, String> {
    Optional<OtpRecord> findByEmail(String email);
    void deleteByEmail(String email);

    // New methods for optimization
    List<OtpRecord> findByExpirationTimeBefore(LocalDateTime dateTime);
    long countByExpirationTimeAfter(LocalDateTime dateTime);
}

