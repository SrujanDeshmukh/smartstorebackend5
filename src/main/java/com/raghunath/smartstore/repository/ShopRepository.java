package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.dto.ShopListProjection;
import com.raghunath.smartstore.entity.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ShopRepository extends MongoRepository<Shop, String> {

    @Query(value = "{}",
            fields = "{ 'shopName' : 1, 'city' : 1, 'isApproved' : 1, 'rating' : 1, 'openingTime' : 1, 'closingTime' : 1, 'bannerUrl' : 1, 'isOpen' : 1, '_id' : 1 }")
    List<ShopListProjection> findAllProjected();

    @Query(value = "{ 'city' : ?0, 'isActive' : true }",
            fields = "{ 'shopName' : 1, 'city' : 1, 'isApproved' : 1, 'rating' : 1, 'openingTime' : 1, 'closingTime' : 1, 'bannerUrl' : 1, 'isOpen' : 1, '_id' : 1 }")
    List<ShopListProjection> findByCityProjection(String city);

    // Other methods unchanged
    List<Shop> findByVendorIdAndIsActive(String vendorId, Boolean isActive);
    List<Shop> findByVendorId(String vendorId);
    Optional<Shop> findByShopName(String shopName);
    List<Shop> findByCity(String city);
    List<Shop> findByCityAndIsActiveTrue(String city);
    long countByCity(String city);
}
