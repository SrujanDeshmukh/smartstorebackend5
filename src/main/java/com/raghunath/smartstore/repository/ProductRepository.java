package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    /**
     * Find products by vendor, shop and active status
     */
    List<Product> findByVendorIdAndShopIdAndIsActive(String vendorId, String shopId, Boolean isActive);
    /**
     * Find products by shop and active status
     */
    List<Product> findByShopIdAndIsActive(String shopId, Boolean isActive);
    /**
     * Find products by vendor and active status
     */
    List<Product> findByVendorIdAndIsActive(String vendorId, Boolean isActive);
    /**
     * Find all products by shop (including inactive)
     */
    List<Product> findByShopId(String shopId);
    /**
     * Find all products by vendor (including inactive)
     */
    List<Product> findByVendorId(String vendorId);
    /**
     * Count active products in a shop
     */
    long countByShopIdAndIsActiveTrue(String shopId);
    /**
     * Find active products by shop
     */
    List<Product> findByShopIdAndIsActiveTrue(String shopId);


    /**
     * Count products in a shop by active status
     * Used by: ProductService.getProductCountByShop()
     */
    Integer countByShopIdAndIsActive(String shopId, Boolean isActive);
    /**
     * Find products by shop, category and active status
     * Used by: ProductService.getProductsByCategory()
     */
    List<Product> findByShopIdAndCategoryAndIsActive(String shopId, String category, Boolean isActive);
    /**
     * Search products by name (case-insensitive) in a shop
     * Used by: ProductService.searchProductsByName()
     */
    List<Product> findByShopIdAndProductNameContainingIgnoreCaseAndIsActive(String shopId, String searchTerm, Boolean isActive);
    /**
     * Find products from multiple shops (for city-wide search)
     * Used by: ProductService.getProductsByCity()
     */
    List<Product> findByShopIdInAndIsActive(List<String> shopIds, Boolean isActive);
    /**
     * Find products by vendor with specific quantity
     * Used by: ProductService.getOutOfStockProducts() (quantity = 0)
     */
    List<Product> findByVendorIdAndQuantityAndIsActive(String vendorId, Integer quantity, Boolean isActive);

    /**
     * Find products by vendor with quantity less than threshold
     * Used by: ProductService.getLowStockProducts()
     */
    List<Product> findByVendorIdAndQuantityLessThanAndIsActive(String vendorId, Integer threshold, Boolean isActive);
    /**
     * Find products by category across all shops (active only)
     * Used by: Search products by category globally
     */
    List<Product> findByCategoryAndIsActiveTrue(String category);
    /**
     * Find products by vendor and category
     * Used by: Vendor-specific category filtering
     */
    List<Product> findByVendorIdAndCategoryAndIsActive(String vendorId, String category, Boolean isActive);
    /**
     * Search products by name across all shops (case-insensitive)
     * Used by: Global product search
     */
    List<Product> findByProductNameContainingIgnoreCaseAndIsActiveTrue(String searchTerm);

    /**
     * Find products by price range in a shop
     * Used by: Price filter in product listing
     */
    List<Product> findByShopIdAndPriceBetweenAndIsActive(
            String shopId, Double minPrice, Double maxPrice, Boolean isActive);

    /**
     * Find products with discount in a shop
     * Used by: Show products on offer
     */
    List<Product> findByShopIdAndDiscountGreaterThanAndIsActive(
            String shopId, Double discount, Boolean isActive);

    /**
     * Find products by brand in a shop
     * Used by: Brand filter in product listing
     */
    List<Product> findByShopIdAndBrandAndIsActive(String shopId, String brand, Boolean isActive);

    /**
     * Count products by category in a shop
     * Used by: Category-wise product count for vendor dashboard
     */
    long countByShopIdAndCategoryAndIsActiveTrue(String shopId, String category);

    /**
     * Find top N products by shop (can be used for featured products)
     * Note: Requires @Query annotation for sorting and limiting
     */
    List<Product> findTop10ByShopIdAndIsActiveTrueOrderByCreatedAtDesc(String shopId);

    List<Product> findByVendorIdAndQuantityAndIsActive(String id, int i, boolean b);
}
