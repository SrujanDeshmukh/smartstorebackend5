package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.EmployeeRequest;
import com.raghunath.smartstore.dto.EmployeeResponse;
import com.raghunath.smartstore.entity.Employee;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final VendorService vendorService;
    private final ShopService shopService;
    private final PasswordEncoder passwordEncoder;

    // ================================
    // ADD EMPLOYEE (Enhanced)
    // ================================
    @Transactional
    public EmployeeResponse addEmployee(String vendorEmail, String shopId, EmployeeRequest request) {
        try {
            log.info("Adding employee for vendor: {} to shop: {}", vendorEmail, shopId);

            // ✅ Step 1: Validate vendor exists
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

            // ✅ Step 2: Validate shop belongs to vendor
            shopService.getShopById(shopId);

            // ✅ Step 3: Check if email already exists
            if (employeeRepository.existsByEmail(request.getEmail())) {
                log.warn("Email already exists: {}", request.getEmail());
                throw new RuntimeException("Email is already registered");
            }

            // ✅ Step 4: Create employee entity
            Employee employee = Employee.builder()
                    .vendorId(vendor.getId())
                    .shopId(shopId)
                    .employeeName(request.getEmployeeName())
                    .email(request.getEmail().toLowerCase().trim())
                    .password(passwordEncoder.encode(request.getEmployeePassword()))
                    .employeeMobile(request.getEmployeeMobile())
                    .employeeRole(request.getEmployeeRole() != null ? request.getEmployeeRole() : "STAFF")
                    .department(request.getDepartment())
                    .basicSalary(request.getBasicSalary())
                    .shiftTiming(request.getShiftTiming())
                    .isActive(true)
                    .isVerified(false)
                    .emailVerified(false)
                    .mobileVerified(false)
                    .onProbation(true)
                    .canProcessOrders(true)
                    .canManageInventory(false)
                    .canAccessReports(false)
                    .isSupervisor(false)
                    .failedLoginAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .hiredAt(LocalDateTime.now())
                    .probationEndDate(LocalDateTime.now().plusMonths(6))
                    .build();

            // ✅ Step 5: Generate employee code
            employee.generateEmployeeCode();

            // ✅ Step 6: Save employee
            Employee savedEmployee = employeeRepository.save(employee);

            log.info("✅ Employee added successfully: {} (ID: {})", savedEmployee.getEmail(), savedEmployee.getId());

            return mapToResponse(savedEmployee);

        } catch (Exception e) {
            log.error("❌ Failed to add employee: {}", e.getMessage());
            throw new RuntimeException("Failed to add employee: " + e.getMessage());
        }
    }

    // ================================
    // GET EMPLOYEES BY SHOP
    // ================================
    public List<EmployeeResponse> getShopEmployees(String vendorEmail, String shopId) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
            List<Employee> employees = employeeRepository.findByVendorIdAndShopIdAndIsActive(
                    vendor.getId(), shopId, true);

            log.info("Retrieved {} employees for shop: {}", employees.size(), shopId);

            return employees.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get shop employees: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve employees: " + e.getMessage());
        }
    }

    // ================================
    // GET ALL VENDOR EMPLOYEES
    // ================================
    public List<EmployeeResponse> getAllVendorEmployees(String vendorEmail) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
            List<Employee> employees = employeeRepository.findByVendorIdAndIsActive(
                    vendor.getId(), true);

            log.info("Retrieved {} employees for vendor: {}", employees.size(), vendorEmail);

            return employees.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get vendor employees: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve employees: " + e.getMessage());
        }
    }

    // ================================
    // GET EMPLOYEE BY ID
    // ================================
    public Employee getEmployeeById(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));
    }

    // ================================
    // GET EMPLOYEE BY EMAIL
    // ================================
    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Employee not found with email: " + email));
    }

    // ================================
    // UPDATE EMPLOYEE
    // ================================
    @Transactional
    public EmployeeResponse updateEmployee(String employeeId, EmployeeRequest request) {
        try {
            Employee employee = getEmployeeById(employeeId);

            // Update fields
            if (request.getEmployeeName() != null) {
                employee.setEmployeeName(request.getEmployeeName());
            }
            if (request.getEmail() != null) {
                // Check if new email already exists
                if (!employee.getEmail().equals(request.getEmail()) &&
                        employeeRepository.existsByEmail(request.getEmail())) {
                    throw new RuntimeException("Email is already registered");
                }
                employee.setEmail(request.getEmail().toLowerCase().trim());
            }
            if (request.getEmployeeMobile() != null) {
                employee.setEmployeeMobile(request.getEmployeeMobile());
            }
            if (request.getEmployeePassword() != null && !request.getEmployeePassword().isEmpty()) {
                employee.setPassword(passwordEncoder.encode(request.getEmployeePassword()));
                employee.setPasswordUpdatedAt(LocalDateTime.now());
            }
            if (request.getEmployeeRole() != null) {
                employee.setEmployeeRole(request.getEmployeeRole());
            }
            if (request.getDepartment() != null) {
                employee.setDepartment(request.getDepartment());
            }
            if (request.getBasicSalary() != null) {
                employee.setBasicSalary(request.getBasicSalary());
            }
            if (request.getShiftTiming() != null) {
                employee.setShiftTiming(request.getShiftTiming());
            }

            employee.setUpdatedAt(LocalDateTime.now());
            Employee updatedEmployee = employeeRepository.save(employee);

            log.info("✅ Employee updated successfully: {}", employeeId);

            return mapToResponse(updatedEmployee);

        } catch (Exception e) {
            log.error("❌ Failed to update employee: {}", e.getMessage());
            throw new RuntimeException("Failed to update employee: " + e.getMessage());
        }
    }

    // ================================
    // DELETE/DEACTIVATE EMPLOYEE
    // ================================
    @Transactional
    public String deleteEmployee(String employeeId) {
        try {
            Employee employee = getEmployeeById(employeeId);
            employee.deactivate("Removed by vendor");
            employeeRepository.save(employee);

            log.info("✅ Employee deactivated successfully: {}", employeeId);
            return "Employee deleted successfully";

        } catch (Exception e) {
            log.error("❌ Failed to delete employee: {}", e.getMessage());
            throw new RuntimeException("Failed to delete employee: " + e.getMessage());
        }
    }

    // ================================
    // EMPLOYEE STATISTICS
    // ================================
    public EmployeeStats getEmployeeStats(String shopId) {
        try {
            long totalEmployees = employeeRepository.countByShopIdAndIsActive(shopId, true);
            List<Employee> employees = employeeRepository.findByShopIdAndIsActive(shopId, true);

            long onProbation = employees.stream().filter(Employee::isOnProbation).count();
            long supervisors = employees.stream().filter(Employee::isSupervisor).count();

            return new EmployeeStats(totalEmployees, onProbation, supervisors);

        } catch (Exception e) {
            log.error("Failed to get employee stats: {}", e.getMessage());
            return new EmployeeStats(0, 0, 0);
        }
    }

    // ================================
    // HELPER METHODS
    // ================================
    public EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())
                .email(employee.getEmail())
                .employeeMobile(employee.getEmployeeMobile())
                .shopId(employee.getShopId())
                .vendorId(employee.getVendorId())
                .employeeRole(employee.getEmployeeRole())
                .department(employee.getDepartment())
                .isActive(employee.getIsActive())
                .isVerified(employee.getIsVerified())
                .onProbation(employee.getOnProbation())
                .basicSalary(employee.getBasicSalary())
                .shiftTiming(employee.getShiftTiming())
                .createdAt(employee.getCreatedAt())
                .lastLoginAt(employee.getLastLoginAt())
                .canProcessOrders(employee.getCanProcessOrders())
                .canManageInventory(employee.getCanManageInventory())
                .canAccessReports(employee.getCanAccessReports())
                .isSupervisor(employee.getIsSupervisor())
                .totalSales(employee.getTotalSales())
                .ordersProcessed(employee.getOrdersProcessed())
                .customerRating(employee.getCustomerRating())
                .build();
    }

    // ================================
    // EMPLOYEE STATS CLASS
    // ================================
    public static class EmployeeStats {
        private final long totalEmployees;
        private final long onProbation;
        private final long supervisors;

        public EmployeeStats(long totalEmployees, long onProbation, long supervisors) {
            this.totalEmployees = totalEmployees;
            this.onProbation = onProbation;
            this.supervisors = supervisors;
        }

        public long getTotalEmployees() { return totalEmployees; }
        public long getOnProbation() { return onProbation; }
        public long getSupervisors() { return supervisors; }
    }
}
