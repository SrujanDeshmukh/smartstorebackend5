package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.EmployeeRequest;
import com.raghunath.smartstore.dto.EmployeeResponse;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/vendor/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;

    // ================================
    // ADD EMPLOYEE TO SHOP
    // ================================
    @PostMapping("/shop/{shopId}")
    public ResponseEntity<Map<String, Object>> addEmployee(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId,
            @Valid @RequestBody EmployeeRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Extract and validate token
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String role = jwtUtil.extractRole(actualToken);

            if (!"VENDOR".equals(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Vendor role required.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            String email = jwtUtil.extractUsername(actualToken);
            EmployeeResponse employee = employeeService.addEmployee(email, shopId, request);

            response.put("success", true);
            response.put("message", "Employee added successfully");
            response.put("employee", employee);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to add employee: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ================================
    // GET ALL EMPLOYEES FOR A SHOP
    // ================================
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Map<String, Object>> getShopEmployees(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        Map<String, Object> response = new HashMap<>();

        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String email = jwtUtil.extractUsername(actualToken);

            List<EmployeeResponse> employees = employeeService.getShopEmployees(email, shopId);

            response.put("success", true);
            response.put("count", employees.size());
            response.put("employees", employees);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get shop employees: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ================================
    // GET ALL VENDOR EMPLOYEES (All Shops)
    // ================================
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllVendorEmployees(
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String email = jwtUtil.extractUsername(actualToken);

            List<EmployeeResponse> employees = employeeService.getAllVendorEmployees(email);

            response.put("success", true);
            response.put("count", employees.size());
            response.put("employees", employees);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get vendor employees: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ================================
    // GET EMPLOYEE BY ID
    // ================================
    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployee(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            EmployeeResponse employee = employeeService.mapToResponse(
                    employeeService.getEmployeeById(employeeId));

            response.put("success", true);
            response.put("employee", employee);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get employee: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // ================================
    // UPDATE EMPLOYEE
    // ================================
    @PutMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            EmployeeResponse employee = employeeService.updateEmployee(employeeId, request);

            response.put("success", true);
            response.put("message", "Employee updated successfully");
            response.put("employee", employee);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update employee: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ================================
    // DELETE/DEACTIVATE EMPLOYEE
    // ================================
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String message = employeeService.deleteEmployee(employeeId);

            response.put("success", true);
            response.put("message", message);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete employee: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ================================
    // GET EMPLOYEE STATISTICS
    // ================================
    @GetMapping("/shop/{shopId}/stats")
    public ResponseEntity<Map<String, Object>> getEmployeeStats(@PathVariable String shopId) {
        Map<String, Object> response = new HashMap<>();

        try {
            EmployeeService.EmployeeStats stats = employeeService.getEmployeeStats(shopId);

            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get employee stats: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
