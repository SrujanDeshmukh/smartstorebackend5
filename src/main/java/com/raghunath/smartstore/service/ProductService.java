package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.ProductRequest;
import com.raghunath.smartstore.entity.Product;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorService vendorService;
    private final ShopService shopService;

    public String addProduct(String vendorEmail, String shopId, ProductRequest request) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

        // Validate shop belongs to vendor
        shopService.getShopById(shopId);

        Product product = new Product();
        product.setVendorId(vendor.getId());
        product.setShopId(shopId);
        product.setProductName(request.getProductName());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());
        product.setPhoto(request.getPhoto());
        product.setDescription(request.getDescription());

        productRepository.save(product);
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

    public String updateProduct(String productId, ProductRequest request) {
        Product product = getProductById(productId);
        product.setProductName(request.getProductName());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());
        product.setPhoto(request.getPhoto());
        product.setDescription(request.getDescription());
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
        return "Product updated successfully";
    }

    public String deleteProduct(String productId) {
        Product product = getProductById(productId);
        product.setIsActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        return "Product deleted successfully";
    }
}
