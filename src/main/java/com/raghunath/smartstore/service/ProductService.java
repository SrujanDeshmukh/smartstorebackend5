package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.ProductRequest;
import com.raghunath.smartstore.entity.Product;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.ProductRepository;
import com.raghunath.smartstore.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorService vendorService;
    private final ShopRepository shopRepository;

    public String addProduct(String vendorEmail, String shopId, ProductRequest request) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        if(!shop.getVendorId().equals(vendor.getId())){
            throw new RuntimeException("Shop does not belong to this vendor");
        }

        Product product = new Product();
        product.setVendorId(vendor.getId());
        product.setShopId(shopId);
        product.setProductName(request.getProductName());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());

        // Set new fields
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setDiscount(Double.valueOf(request.getDiscount()));
        product.setCategory(request.getCategory());

        product.setIsActive(true);

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        updateShopProductCount(shopId);

        log.info("Product added: {} to shop: {} by vendor: {}", product.getProductName(), shopId, vendorEmail);

        return "Product added successfully";
    }

    public List<Product> getShopProducts(String shopId) {
        return productRepository.findByShopIdAndIsActive(shopId, true);
    }

    public List<Product> getVendorProducts(String vendorEmail) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return productRepository.findByVendorIdAndIsActive(vendor.getId(), true);
    }

    public Product getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Integer getProductCountByShop(String shopId){
        return (int)productRepository.countByShopIdAndIsActiveTrue(shopId);
    }

    public String updateProduct(String productId, ProductRequest request) {
        Product product = getProductById(productId);
        product.setProductName(request.getProductName());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());

        // Update new fields
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setDiscount(Double.valueOf(request.getDiscount()));
        product.setCategory(request.getCategory());

        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        log.info("Product updated: {}", productId);

        return "Product updated successfully";
    }

    public String deleteProduct(String productId) {
        Product product = getProductById(productId);
        String shopId = product.getShopId();

        product.setIsActive(false);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        updateShopProductCount(shopId);

        log.info("Product deleted (soft): {}", productId);

        return "Product deleted successfully";
    }

    /**
     * Get products by category for a shop
     */
    public List<Product> getProductsByCategory(String shopId, String category) {
        return productRepository.findByShopIdAndCategoryAndIsActive(shopId, category, true);
    }

    /**
     * Search products by name in a shop
     */
    public List<Product> searchProductsByName(String shopId, String searchTerm) {
        return productRepository.findByShopIdAndProductNameContainingIgnoreCaseAndIsActive(
                shopId, searchTerm, true);
    }

    /**
     * Get all products in a city (across all shops)
     */
    public List<Product> getProductsByCity(String city) {
        // First get all shops in the city
        List<Shop> shops = shopRepository.findByCityAndIsActiveTrue(city);

        // Get shop IDs
        List<String> shopIds = shops.stream()
                .map(Shop::getId)
                .toList();

        // Get all products from these shops
        return productRepository.findByShopIdInAndIsActive(shopIds, true);
    }

    /**
     * Update shop's total product count
     * Called after adding/deleting products
     */
    private void updateShopProductCount(String shopId) {

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        Integer productCount = getProductCountByShop(shopId);
        shop.setTotalProducts(productCount);
        shop.setUpdatedAt(LocalDateTime.now());

        shopRepository.save(shop);

        log.debug("Updated product count for shop {}: {}", shopId, productCount);
    }

    /**
     * Validate product belongs to vendor (for update/delete operations)
     */
    public void validateProductOwnership(String productId, String vendorEmail) {

        Product product = getProductById(productId);
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

        if (!product.getVendorId().equals(vendor.getId())) {
            throw new RuntimeException("Product does not belong to this vendor");
        }
    }

    /**
     * Get out of stock products for a vendor
     */
    public List<Product> getOutOfStockProducts(String vendorEmail) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return productRepository.findByVendorIdAndQuantityAndIsActive(
                vendor.getId(), 0, true);
    }

    /**
     * Get low stock products (quantity < threshold)
     */
    public List<Product> getLowStockProducts(String vendorEmail, Integer threshold) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return productRepository.findByVendorIdAndQuantityLessThanAndIsActive(
                vendor.getId(), threshold, true);
    }

    /**
     * Bulk update product prices (for discounts/offers)
     */
    public String bulkUpdateDiscount(String shopId, String category, Double discount) {

        List<Product> products = productRepository.findByShopIdAndCategoryAndIsActive(
                shopId, category, true);

        products.forEach(product -> {
            product.setDiscount(discount);
            product.setUpdatedAt(LocalDateTime.now());
        });

        productRepository.saveAll(products);

        log.info("Bulk discount update: {}% on {} category in shop {}",
                discount, category, shopId);

        return String.format("Discount of %s%% applied to %d products",
                discount, products.size());
    }
}
