package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByVendorIdAndShopIdAndStatus(String vendorId, String shopId, String status);
    List<Order> findByVendorIdAndShopId(String vendorId, String shopId);
}
