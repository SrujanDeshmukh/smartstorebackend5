package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.EmployeeRequest;
import com.raghunath.smartstore.entity.Employee;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;

    @PostMapping("/shop/{shopId}")
    public ResponseEntity<String> addEmployee(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId,
            @Valid @RequestBody EmployeeRequest request) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtil.extractRole(actualToken);

        if (!"VENDOR".equals(role)) {
            throw new RuntimeException("Access denied. Vendor role required.");
        }

        String email = jwtUtil.extractUsername(actualToken);
        return ResponseEntity.ok(employeeService.addEmployee(email, shopId, request));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Employee>> getShopEmployees(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(employeeService.getShopEmployees(email, shopId));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<Employee> getEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getEmployeeById(employeeId));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<String> updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeRequest request) {

        return ResponseEntity.ok(employeeService.updateEmployee(employeeId, request));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.deleteEmployee(employeeId));
    }
}
