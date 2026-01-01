package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Advertisement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends MongoRepository<Advertisement, String> {

    // Vendor: Get all ads for their shop
    List<Advertisement> findByVendorIdAndShopId(String vendorId, String shopId);

    // User: Get active approved ads for a shop (within date range)
    List<Advertisement> findByShopIdAndIsActiveAndIsApprovedAndStartDateBeforeAndEndDateAfter(
            String shopId,
            Boolean isActive,
            Boolean isApproved,
            LocalDateTime currentDate1,
            LocalDateTime currentDate2
    );
}
