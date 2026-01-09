package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.ShopRequest;
import com.raghunath.smartstore.dto.ShopResponse;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.exception.NotFoundException;
import com.raghunath.smartstore.repository.ProductRepository;
import com.raghunath.smartstore.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final VendorService vendorService;
    private final ProductRepository productRepository;

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

        shop.setIsActive(true);
        shop.setIsApproved(false);
        shop.setCreatedAt(LocalDateTime.now());

        log.info("Shop '{}' created in city: {}", shop.getShopName(),shop.getCity());
        shopRepository.save(shop);
        return "Shop added successfully";
    }

    public List<ShopResponse> getShopsByCity(String city){
        log.info("Fetching shops for city: {}");

//        List<Shop> shops = shopRepository.findByCityAndIsActiveTrueAndIsApprovedTrue(city);
        List<Shop> shops = shopRepository.findByCityAndIsActiveTrue(city);

        if(shops.isEmpty()){
            log.warn("No shops found in city: {}", city);
            return List.of();
        }

        log.info("Found {} shops in {}", shops.size(), city);

        return shops.stream()
                .map(this::convertToShopResponse)
                .collect(Collectors.toList());
    }

    public List<Shop> getVendorShops(String vendorEmail) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return shopRepository.findByVendorIdAndIsActive(vendor.getId(), true);
    }

    public Shop getShopById(String shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
    }

    public String updateShop(String shopId, ShopRequest request) {
        Shop shop = getShopById(shopId);
        shop.setShopName(request.getShopName());
        shop.setCity(request.getCity());
        shop.setShopAddress(request.getShopAddress());
        shop.setShopType(request.getShopType());
        shop.setContactNumber(request.getContactNumber());
        shop.setDescription(request.getDescription());
        shop.setGstNumber(request.getGstNumber());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        shop.setUpdatedAt(LocalDateTime.now());

        shopRepository.save(shop);
        return "Shop updated successfully";
    }

    public ShopResponse getShopDetailsById(String shopId){
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        return convertToShopResponse(shop);
    }

    public String deleteShop(String shopId) {
        Shop shop = getShopById(shopId);
        shop.setIsActive(false);
        shopRepository.save(shop);
        return "Shop deleted successfully";
    }

    public String approveShop(String shopId){

        Shop shop = getShopById(shopId);
        shop.setIsApproved(true);
        shop.setUpdatedAt(LocalDateTime.now());

        shopRepository.save(shop);

        log.info("Shop '{}' approved by admin", shop.getShopName());

        return "Shop approved successfully";
    }

    public String rejectShop(String shopId){

        Shop shop = getShopById(shopId);
        shop.setIsApproved(false);
        shop.setIsActive(false);
        shop.setUpdatedAt(LocalDateTime.now());

        shopRepository.save(shop);

        log.info("Shop '{}' rejected by admin", shop.getShopName());

        return "Shop rejected";
    }

    private ShopResponse convertToShopResponse(Shop shop){

        ShopResponse response = new ShopResponse();

        response.setShopId(shop.getId());
        response.setShopName(shop.getShopName());
        response.setDescription(shop.getDescription());
        response.setCity(shop.getCity());
        response.setAddress(shop.getShopAddress());
        response.setPhone(shop.getContactNumber());
        response.setShopType(shop.getShopType());
        response.setGstNumber(shop.getGstNumber());

        response.setLatitude(shop.getLatitude());
        response.setLongitude(shop.getLongitude());

        response.setIsActive(shop.getIsActive());
        shop.setIsApproved(shop.getIsApproved());

        // To get vendor details from Vendor Collection
        try{
            Vendor vendor = vendorService.getVendorById(shop.getVendorId());
            response.setVendorName(vendor.getFullName());
            response.setVendorId(vendor.getId());
            response.setVendorEmail(vendor.getEmail());
            response.setVendorPhone(vendor.getMobile());
        }
        catch (Exception e){
            log.error("Error fetching vendor details from shop {}: {}", shop.getId(), e.getMessage());
            response.setVendorName("Unknown");
        }

        // To count total products in this shop
        try{
            long productCount = productRepository.countByShopIdAndIsActiveTrue(shop.getId());
            response.setTotalProducts((int) productCount);
        }
        catch(Exception e) {
            log.error("Error counting products for shop {}: {}", shop.getId(), e.getMessage());
            response.setTotalProducts(0);
        }

        response.setRating(0.0);

        return response;
    }
}
