package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.OtpRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<OtpRecord, String> {
    Optional<OtpRecord> findByEmail(String email);
    void deleteByEmail(String email);
}
