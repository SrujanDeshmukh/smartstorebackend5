package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.AdvertisementRequest;
import com.raghunath.smartstore.entity.Advertisement;
import com.raghunath.smartstore.entity.Product;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final VendorService vendorService;
    private final ShopService shopService;
    private final ProductService productService;

    public String createAdvertisement(String vendorEmail, String shopId, AdvertisementRequest request) {
        try {
            // Get vendor by email
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

            // Validate shop exists and belongs to vendor
            Shop shop = shopService.getShopById(shopId);
            if (!shop.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Shop does not belong to this vendor");
            }

            // Validate shop is active
            if (!shop.getIsActive()) {
                throw new IllegalArgumentException("Cannot create advertisement for inactive shop");
            }

            // Validate all product IDs exist and belong to this shop
            List<String> productIds = request.getProductIds();
            if (productIds == null || productIds.isEmpty()) {
                throw new IllegalArgumentException("At least one product must be selected for advertisement");
            }

            for (String productId : productIds) {
                try {
                    Product product = productService.getProductById(productId);
                    if (!product.getShopId().equals(shopId)) {
                        throw new IllegalArgumentException("Product " + productId + " does not belong to this shop");
                    }
                    if (!product.getIsActive()) {
                        throw new IllegalArgumentException("Cannot create advertisement for inactive product: " + productId);
                    }
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Invalid product ID: " + productId + ". " + e.getMessage());
                }
            }

            // Validate offer end date
            if (request.getOfferEndDate() != null && request.getOfferEndDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Offer end date must be in the future");
            }

            // Create new advertisement
            Advertisement advertisement = new Advertisement();
            advertisement.setVendorId(vendor.getId());
            advertisement.setShopId(shopId);
            advertisement.setProductIds(request.getProductIds());
            advertisement.setDescription(request.getDescription());
            advertisement.setOfferEndDate(request.getOfferEndDate());

            // Save advertisement
            advertisementRepository.save(advertisement);
            return "Advertisement created successfully";

        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Advertisement already exists with the same configuration: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to create advertisement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred while creating advertisement: " + e.getMessage(), e);
        }
    }

    public List<Advertisement> getShopAdvertisements(String vendorEmail, String shopId) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

            // Validate shop belongs to vendor
            Shop shop = shopService.getShopById(shopId);
            if (!shop.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Shop does not belong to this vendor");
            }

            return advertisementRepository.findByVendorIdAndShopIdAndIsActive(vendor.getId(), shopId, true);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to retrieve shop advertisements: " + e.getMessage(), e);
        }
    }

    public Advertisement getAdvertisementById(String advertisementId) {
        if (advertisementId == null || advertisementId.trim().isEmpty()) {
            throw new IllegalArgumentException("Advertisement ID cannot be null or empty");
        }

        return advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Advertisement not found with ID: " + advertisementId));
    }

    public String updateAdvertisement(String advertisementId, AdvertisementRequest request) {
        try {
            Advertisement advertisement = getAdvertisementById(advertisementId);

            // Validate product IDs if provided
            if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
                for (String productId : request.getProductIds()) {
                    try {
                        Product product = productService.getProductById(productId);
                        if (!product.getShopId().equals(advertisement.getShopId())) {
                            throw new IllegalArgumentException("Product " + productId + " does not belong to this shop");
                        }
                        if (!product.getIsActive()) {
                            throw new IllegalArgumentException("Cannot add inactive product to advertisement: " + productId);
                        }
                    } catch (RuntimeException e) {
                        throw new IllegalArgumentException("Invalid product ID: " + productId + ". " + e.getMessage());
                    }
                }
                advertisement.setProductIds(request.getProductIds());
            }

            // Update description if provided
            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                advertisement.setDescription(request.getDescription());
            }

            // Update offer end date if provided
            if (request.getOfferEndDate() != null) {
                if (request.getOfferEndDate().isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Offer end date must be in the future");
                }
                advertisement.setOfferEndDate(request.getOfferEndDate());
            }

            advertisementRepository.save(advertisement);
            return "Advertisement updated successfully";

        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Advertisement update violates unique constraints: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to update advertisement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred while updating advertisement: " + e.getMessage(), e);
        }
    }

    public String deleteAdvertisement(String advertisementId) {
        try {
            Advertisement advertisement = getAdvertisementById(advertisementId);
            advertisement.setIsActive(false);
            advertisementRepository.save(advertisement);
            return "Advertisement deleted successfully";
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to delete advertisement: " + e.getMessage(), e);
        }
    }

    // UPDATED HELPER METHODS (Using existing repository methods only)

    public List<Advertisement> getActiveAdvertisementsByVendor(String vendorEmail) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
            return advertisementRepository.findByVendorIdAndIsActive(vendor.getId(), true);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to retrieve vendor advertisements: " + e.getMessage(), e);
        }
    }

    public List<Advertisement> getExpiredAdvertisements(String vendorEmail, String shopId) {
        try {
            Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

            // Validate shop belongs to vendor
            Shop shop = shopService.getShopById(shopId);
            if (!shop.getVendorId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Shop does not belong to this vendor");
            }

            return advertisementRepository.findByVendorIdAndShopIdAndIsActiveAndOfferEndDateBefore(
                    vendor.getId(), shopId, true, LocalDateTime.now());
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to retrieve expired advertisements: " + e.getMessage(), e);
        }
    }

    public String activateAdvertisement(String advertisementId) {
        try {
            Advertisement advertisement = getAdvertisementById(advertisementId);
            advertisement.setIsActive(true);
            advertisementRepository.save(advertisement);
            return "Advertisement activated successfully";
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to activate advertisement: " + e.getMessage(), e);
        }
    }
}
