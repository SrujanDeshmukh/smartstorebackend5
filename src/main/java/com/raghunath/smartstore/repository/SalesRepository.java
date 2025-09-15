package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Sales;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesRepository extends MongoRepository<Sales, String> {
    List<Sales> findByVendorIdAndShopId(String vendorId, String shopId);
    List<Sales> findByVendorIdAndShopIdAndSaleDateBetween(String vendorId, String shopId, LocalDateTime startDate, LocalDateTime endDate);
}
