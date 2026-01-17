package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.ShopListProjection;
import com.raghunath.smartstore.dto.ShopRequest;
import com.raghunath.smartstore.dto.ShopResponse;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.exception.NotFoundException;
import com.raghunath.smartstore.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final VendorService vendorService;
    private final ProductService productService;

    /**
     * Add new shop ✅ FIXED: Removed duplicate save()
     */
    public String addShop(String vendorEmail, ShopRequest request) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

        Shop shop = new Shop();
        shop.setVendorId(vendor.getId());
        shop.setShopName(request.getShopName());
        shop.setCity(request.getCity());
        shop.setShopAddress(request.getShopAddress());
        shop.setShopType(request.getShopType());
        shop.setContactNumber(request.getContactNumber());
        shop.setDescription(request.getDescription());
        shop.setGstNumber(request.getGstNumber());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        shop.setEmail(request.getEmail());
        shop.setOpeningTime(request.getOpeningTime());
        shop.setClosingTime(request.getClosingTime());
        shop.setBannerUrl(request.getBannerUrl());  // ✅ Banner set once

        // Store vendor info for faster access
        shop.setVendorName(vendor.getFullName());
        shop.setVendorEmail(vendor.getEmail());
        shop.setVendorPhone(vendor.getMobile());

        // Set defaults
        shop.setIsActive(true);
        shop.setIsApproved(false);
        shop.setTotalProducts(0);
        shop.setRating(0.0);
        shop.setCreatedAt(LocalDateTime.now());
        shop.setUpdatedAt(LocalDateTime.now());

        shopRepository.save(shop);  // ✅ Single save() - removed duplicate

        log.info("Shop '{}' created by vendor: {} in city: {}",
                shop.getShopName(), vendorEmail, shop.getCity());

        return "Shop added successfully. Waiting for admin approval.";
    }

    /**
     * Update shop ✅ FIXED: Banner URL not updated
     */
    public String updateShop(String shopId, ShopRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        shop.setShopName(request.getShopName());
        shop.setCity(request.getCity());
        shop.setShopAddress(request.getShopAddress());
        shop.setShopType(request.getShopType());
        shop.setContactNumber(request.getContactNumber());
        shop.setDescription(request.getDescription());
        shop.setGstNumber(request.getGstNumber());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        shop.setEmail(request.getEmail());
        shop.setOpeningTime(request.getOpeningTime());
        shop.setClosingTime(request.getClosingTime());
        shop.setUpdatedAt(LocalDateTime.now());  // ✅ Update timestamp

        // ❌ Banner URL NOT updated (set once during creation)

        shopRepository.save(shop);
        log.info("Shop updated: {}", shopId);
        return "Shop updated successfully";
    }

    /**
     * Get shops by city - LIST VIEW ✅ SINGLE DTO with conditional fields
     */
    public List<ShopListProjection> getShopsByCity(String city) {
        List<Shop> shops = shopRepository.findByCityAndIsActiveTrue(city);
        return shopRepository.findByCityProjection(city);
//        return shops.stream()
//                .map(shop -> convertToShopResponse(shop, true))  // ✅ includeBanner=true
//                .collect(Collectors.toList());
    }

    /**
     * Get shop details - DETAIL VIEW ✅ SINGLE DTO without banner
     */
    public ShopResponse getShopDetailsById(String shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        return convertToShopResponse(shop, false);  // ✅ includeBanner=false
    }

    /**
     * ✅ INTERNAL SERVICE - Returns ENTITY (for other services)
     */
    public Shop getShopEntityById(String shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
    }

    /**
     * Get vendor's shops
     */
    public List<Shop> getVendorShops(String vendorEmail) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return shopRepository.findByVendorIdAndIsActive(vendor.getId(), true);
    }

    /**
     * Delete shop (soft delete)
     */
    public String deleteShop(String shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        shop.setIsActive(false);
        shop.setUpdatedAt(LocalDateTime.now());
        shopRepository.save(shop);
        log.info("Shop soft deleted: {}", shopId);
        return "Shop deleted successfully";
    }

    /**
     * ✅ SINGLE CONVERTER METHOD - Conditional field population
     * includeBanner=true → List view (banner, rating, totalProducts)
     * includeBanner=false → Detail view (no banner, no rating, no totalProducts)
     */
    public ShopResponse convertToShopResponse(Shop shop, boolean includeBanner) {

        ShopResponse response = new ShopResponse();

        // ✅ COMMON FIELDS (both list and detail views)
        response.setShopId(shop.getId());
        response.setShopName(shop.getShopName());
        response.setDescription(shop.getDescription());
        response.setCity(shop.getCity());
        response.setAddress(shop.getShopAddress());           // ✅ Fixed mapping
        response.setPhone(shop.getContactNumber());           // ✅ Fixed mapping
        response.setShopType(shop.getShopType());
        response.setGstNumber(shop.getGstNumber());
        response.setLatitude(shop.getLatitude());
        response.setLongitude(shop.getLongitude());
        response.setEmail(shop.getEmail());
        response.setOpeningTime(String.valueOf(shop.getOpeningTime()));
        response.setClosingTime(String.valueOf(shop.getClosingTime()));
        response.setIsOpen(calculateIsOpen(shop.getOpeningTime(), shop.getClosingTime()));
        response.setIsApproved(shop.getIsApproved());

        // ✅ LIST VIEW ONLY FIELDS (getShopsByCity)
        if (includeBanner) {
            response.setBannerUrl(shop.getBannerUrl());
            response.setRating(shop.getRating());
            response.setTotalProducts(productService.getProductCountByShop(shop.getId()));
        }

        // ✅ DETAIL VIEW ONLY - Vendor info (stored in shop for speed)
        if (!includeBanner) {
            response.setVendorName(shop.getVendorName());
            response.setVendorId(shop.getVendorId());
            response.setVendorEmail(shop.getVendorEmail());
            response.setVendorPhone(shop.getVendorPhone());
        }

        return response;
    }

    /**
     * Calculate if shop is currently open
     */
    private Boolean calculateIsOpen(LocalTime openingTime, LocalTime closingTime) {
        if (openingTime == null || closingTime == null) {
            return false;
        }

        LocalTime currentTime = LocalTime.now();

        if (openingTime.isBefore(closingTime)) {
            // Normal hours (9AM - 9PM)
            return !currentTime.isBefore(openingTime) && currentTime.isBefore(closingTime);
        } else {
            // Overnight hours (10PM - 2AM)
            return !currentTime.isBefore(openingTime) || currentTime.isBefore(closingTime);
        }
    }
}
