package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends MongoRepository<Shop, String> {
    List<Shop> findByVendorIdAndIsActive(String vendorId, Boolean isActive);
    List<Shop> findByVendorId(String vendorId);

    Optional<Shop> findByShopName(String shopName);

    List<Shop> findByCity(String city);

    List<Shop> findByCityAndIsActiveTrue(String city);

    //List<Shop> findByCityAndIsActiveTrueAndIsApprovedTrue(String city);

    long countByCity(String city);
}
