package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.EmployeeRequest;
import com.raghunath.smartstore.entity.Employee;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final VendorService vendorService;
    private final ShopService shopService;
    private final PasswordEncoder passwordEncoder;

    public String addEmployee(String vendorEmail, String shopId, EmployeeRequest request) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

        // Validate shop belongs to vendor
        shopService.getShopById(shopId);

        Employee employee = new Employee();
        employee.setVendorId(vendor.getId());
        employee.setShopId(shopId);
        employee.setEmployeeName(request.getEmployeeName());
        employee.setEmployeeMobile(request.getEmployeeMobile());
        employee.setEmployeePassword(passwordEncoder.encode(request.getEmployeePassword()));

        employeeRepository.save(employee);
        return "Employee added successfully";
    }

    public List<Employee> getShopEmployees(String vendorEmail, String shopId) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return employeeRepository.findByVendorIdAndShopIdAndIsActive(vendor.getId(), shopId, true);
    }

    public Employee getEmployeeById(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public String updateEmployee(String employeeId, EmployeeRequest request) {
        Employee employee = getEmployeeById(employeeId);
        employee.setEmployeeName(request.getEmployeeName());
        employee.setEmployeeMobile(request.getEmployeeMobile());
        if (request.getEmployeePassword() != null && !request.getEmployeePassword().isEmpty()) {
            employee.setEmployeePassword(passwordEncoder.encode(request.getEmployeePassword()));
        }

        employeeRepository.save(employee);
        return "Employee updated successfully";
    }

    public String deleteEmployee(String employeeId) {
        Employee employee = getEmployeeById(employeeId);
        employee.setIsActive(false);
        employeeRepository.save(employee);
        return "Employee deleted successfully";
    }
}
