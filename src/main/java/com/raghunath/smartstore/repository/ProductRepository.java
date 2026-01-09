package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByVendorIdAndShopIdAndIsActive(String vendorId, String shopId, Boolean isActive);
    List<Product> findByShopIdAndIsActive(String shopId, Boolean isActive);
    List<Product> findByVendorIdAndIsActive(String vendorId, Boolean isActive);

    List<Product> findByShopId(String shopId);

    List<Product> findByVendorId(String vendorId);

    long countByShopIdAndIsActiveTrue(String shopId);

    List<Product> findByShopIdAndIsActiveTrue(String shopId);
}
