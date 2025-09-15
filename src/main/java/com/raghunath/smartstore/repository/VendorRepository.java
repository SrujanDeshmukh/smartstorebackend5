package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Vendor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VendorRepository extends MongoRepository<Vendor, String> {
    Optional<Vendor> findByEmail(String email);
}
