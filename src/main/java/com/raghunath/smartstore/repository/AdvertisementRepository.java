package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Advertisement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends MongoRepository<Advertisement, String> {

    // Find advertisements by vendor and shop (existing method)
    List<Advertisement> findByVendorIdAndShopIdAndIsActive(String vendorId, String shopId, Boolean isActive);

    // ADD THESE NEW METHODS:

    // Find advertisements by vendor only
    List<Advertisement> findByVendorIdAndIsActive(String vendorId, Boolean isActive);

    // Find expired advertisements (offer end date is in the past)
    List<Advertisement> findByVendorIdAndShopIdAndIsActiveAndOfferEndDateBefore(
            String vendorId, String shopId, Boolean isActive, LocalDateTime dateTime);
}
