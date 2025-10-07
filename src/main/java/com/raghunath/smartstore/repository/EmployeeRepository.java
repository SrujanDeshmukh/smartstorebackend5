package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    List<Employee> findByVendorIdAndShopIdAndIsActive(String vendorId, String shopId, Boolean isActive);

    Optional<Employee> findByEmail(String email);
}
