package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ShopRepository extends MongoRepository<Shop, String> {
    List<Shop> findByVendorIdAndIsActive(String vendorId, Boolean isActive);
    List<Shop> findByVendorId(String vendorId);
}
