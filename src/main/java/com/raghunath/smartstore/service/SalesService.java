package com.raghunath.smartstore.service;

import com.raghunath.smartstore.entity.Sales;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final VendorService vendorService;

    public List<Sales> getShopSales(String vendorEmail, String shopId) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return salesRepository.findByVendorIdAndShopId(vendor.getId(), shopId);
    }

    public List<Sales> getSalesByDateRange(String vendorEmail, String shopId,
                                           LocalDateTime startDate, LocalDateTime endDate) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return salesRepository.findByVendorIdAndShopIdAndSaleDateBetween(
                vendor.getId(), shopId, startDate, endDate);
    }

    // This method would be called when an order is completed
    public void recordSale(String vendorId, String shopId, String productId,
                           String productName, Integer quantity, Double totalAmount) {
        Sales sales = new Sales();
        sales.setVendorId(vendorId);
        sales.setShopId(shopId);
        sales.setProductId(productId);
        sales.setProductName(productName);
        sales.setQuantitySold(quantity);
        sales.setTotalAmount(totalAmount);

        salesRepository.save(sales);
    }
}
