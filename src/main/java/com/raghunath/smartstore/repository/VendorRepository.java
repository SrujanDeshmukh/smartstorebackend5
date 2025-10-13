package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Vendor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends MongoRepository<Vendor, String> {

    // ✅ EXISTING METHODS
    Optional<Vendor> findByEmail(String email);

    // ✅ ADD THIS METHOD TO FIX THE ERROR
    Optional<Vendor> findByMobile(String mobile);

    // ✅ ADDITIONAL USEFUL METHODS
    List<Vendor> findByIsActiveTrue();
    List<Vendor> findByIsVerifiedTrue();
    List<Vendor> findByIsApprovedTrue();

    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);

    long countByIsActiveTrue();
    long countByIsVerifiedTrue();
    long countByIsApprovedTrue();
}
