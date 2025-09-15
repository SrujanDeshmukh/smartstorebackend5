package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByVendorIdAndShopIdAndIsActive(String vendorId, String shopId, Boolean isActive);
    List<Product> findByShopIdAndIsActive(String shopId, Boolean isActive);
    List<Product> findByVendorIdAndIsActive(String vendorId, Boolean isActive);
}
