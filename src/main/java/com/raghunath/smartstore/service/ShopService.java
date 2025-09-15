package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.ShopRequest;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final VendorService vendorService;

    public String addShop(String vendorEmail, ShopRequest request) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);

        Shop shop = new Shop();
        shop.setVendorId(vendor.getId());
        shop.setShopName(request.getShopName());
        shop.setShopAddress(request.getShopAddress());
        shop.setShopType(request.getShopType());
        shop.setContactNumber(request.getContactNumber());
        shop.setDescription(request.getDescription());
        shop.setGstNumber(request.getGstNumber());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());

        shopRepository.save(shop);
        return "Shop added successfully";
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
        shop.setShopAddress(request.getShopAddress());
        shop.setShopType(request.getShopType());
        shop.setContactNumber(request.getContactNumber());
        shop.setDescription(request.getDescription());
        shop.setGstNumber(request.getGstNumber());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());

        shopRepository.save(shop);
        return "Shop updated successfully";
    }

    public String deleteShop(String shopId) {
        Shop shop = getShopById(shopId);
        shop.setIsActive(false);
        shopRepository.save(shop);
        return "Shop deleted successfully";
    }
}
