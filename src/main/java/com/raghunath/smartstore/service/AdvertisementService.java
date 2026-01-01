package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.AdvertisementRequest;
import com.raghunath.smartstore.entity.Advertisement;
import com.raghunath.smartstore.entity.Product;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final VendorService vendorService;
    private final ShopService shopService;
    private final ProductService productService;

    /**
     * CREATE ADVERTISEMENT
     */
    public String createAdvertisement(String vendorEmail, String shopId, AdvertisementRequest request) {
        try {
            log.info("Creating advertisement for shop: {}", shopId);

            // Validate vendor and shop
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
            Shop shop = shopService.getShopById(shopId);

            if (!shop.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Shop does not belong to this vendor");
            }

            if (!shop.getIsActive()) {
                throw new IllegalArgumentException("Cannot create advertisement for inactive shop");
            }

            // Validate date range
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }

            if (request.getStartDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Start date must be in the future");
            }

            // Validate products
            List<String> productIds = request.getProductIds();
            if (productIds == null || productIds.isEmpty()) {
                throw new IllegalArgumentException("At least one product must be selected");
            }

            for (String productId : productIds) {
                Product product = productService.getProductById(productId);
                if (!product.getShopId().equals(shopId)) {
                    throw new IllegalArgumentException("Product " + productId + " does not belong to this shop");
                }
                if (!product.getIsActive()) {
                    throw new IllegalArgumentException("Cannot add inactive product: " + productId);
                }
            }

            // Create advertisement
            Advertisement advertisement = new Advertisement();
            advertisement.setVendorId(vendor.getId());
            advertisement.setShopId(shopId);
            advertisement.setTitle(request.getTitle());
            advertisement.setProductIds(productIds);
            advertisement.setDescription(request.getDescription());
            advertisement.setStartDate(request.getStartDate());
            advertisement.setEndDate(request.getEndDate());
            advertisement.setIsActive(true);
            advertisement.setIsApproved(false);

            advertisementRepository.save(advertisement);

            log.info("Advertisement created successfully with ID: {}", advertisement.getId());
            return "Advertisement created successfully. It will be reviewed and approved soon.";

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating advertisement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create advertisement: " + e.getMessage(), e);
        }
    }

    /**
     * GET VENDOR'S ADVERTISEMENTS FOR A SHOP
     */
    public List<Advertisement> getShopAdvertisements(String vendorEmail, String shopId) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
            Shop shop = shopService.getShopById(shopId);

            if (!shop.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Shop does not belong to this vendor");
            }

            return advertisementRepository.findByVendorIdAndShopId(vendor.getId(), shopId);

        } catch (Exception e) {
            log.error("Error retrieving shop advertisements: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve advertisements", e);
        }
    }

    /**
     * GET SINGLE ADVERTISEMENT BY ID
     */
    public Advertisement getAdvertisementById(String advertisementId) {
        return advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Advertisement not found with ID: " + advertisementId));
    }

    /**
     * UPDATE ADVERTISEMENT
     */
    public String updateAdvertisement(String vendorEmail, String advertisementId, AdvertisementRequest request) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

            Advertisement advertisement = advertisementRepository.findById(advertisementId)
                    .orElseThrow(() -> new RuntimeException("Advertisement not found"));

            // Check ownership
            if (!advertisement.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Advertisement does not belong to this vendor");
            }

            // Can only update if not approved yet
            if (advertisement.getIsApproved()) {
                throw new IllegalArgumentException("Cannot update approved advertisement. Please contact admin.");
            }

            // Validate and update products
            if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
                for (String productId : request.getProductIds()) {
                    Product product = productService.getProductById(productId);
                    if (!product.getShopId().equals(advertisement.getShopId())) {
                        throw new IllegalArgumentException("Product does not belong to this shop");
                    }
                    if (!product.getIsActive()) {
                        throw new IllegalArgumentException("Cannot add inactive product: " + productId);
                    }
                }
                advertisement.setProductIds(request.getProductIds());
            }

            // Update fields
            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                advertisement.setTitle(request.getTitle());
            }

            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                advertisement.setDescription(request.getDescription());
            }

            if (request.getStartDate() != null) {
                if (request.getStartDate().isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Start date must be in the future");
                }
                advertisement.setStartDate(request.getStartDate());
            }

            if (request.getEndDate() != null) {
                if (request.getEndDate().isBefore(request.getStartDate() != null ?
                        request.getStartDate() : advertisement.getStartDate())) {
                    throw new IllegalArgumentException("End date must be after start date");
                }
                advertisement.setEndDate(request.getEndDate());
            }

            advertisement.setUpdatedAt(LocalDateTime.now());
            advertisementRepository.save(advertisement);

            return "Advertisement updated successfully";

        } catch (Exception e) {
            log.error("Error updating advertisement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update advertisement", e);
        }
    }

    /**
     * DELETE ADVERTISEMENT
     */
    public String deleteAdvertisement(String vendorEmail, String advertisementId) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

            Advertisement advertisement = advertisementRepository.findById(advertisementId)
                    .orElseThrow(() -> new RuntimeException("Advertisement not found"));

            // Check ownership
            if (!advertisement.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Advertisement does not belong to this vendor");
            }

            advertisement.setIsActive(false);
            advertisement.setUpdatedAt(LocalDateTime.now());
            advertisementRepository.save(advertisement);

            return "Advertisement deleted successfully";

        } catch (Exception e) {
            log.error("Error deleting advertisement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete advertisement", e);
        }
    }

    /**
     * GET ACTIVE ADVERTISEMENTS FOR SHOP (USER VIEW)
     */
    public List<Advertisement> getActiveAdvertisementsForShop(String shopId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Get active, approved ads within date range and with image
            List<Advertisement> ads = advertisementRepository
                    .findByShopIdAndIsActiveAndIsApprovedAndStartDateBeforeAndEndDateAfter(
                            shopId, true, true, now, now);

            // Filter only ads with images
            return ads.stream()
                    .filter(ad -> ad.getImageUrl() != null && !ad.getImageUrl().isEmpty())
                    .toList();

        } catch (Exception e) {
            log.error("Error retrieving active advertisements: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve advertisements", e);
        }
    }
}
