package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {

    // ✅ Find employee by email (for login)
    Optional<Employee> findByEmail(String email);

    // ✅ Find all active employees for a shop
    List<Employee> findByVendorIdAndShopIdAndIsActive(String vendorId, String shopId, Boolean isActive);

    // ✅ Find all employees by vendor (across all shops)
    List<Employee> findByVendorIdAndIsActive(String vendorId, Boolean isActive);

    // ✅ Find all employees by shop
    List<Employee> findByShopIdAndIsActive(String shopId, Boolean isActive);

    // ✅ Check if email already exists
    boolean existsByEmail(String email);

    // ✅ Count employees in a shop
    long countByShopIdAndIsActive(String shopId, Boolean isActive);

    // ✅ Find employee by employee code
    Optional<Employee> findByEmployeeCode(String employeeCode);
}
