package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.ShopListProjection;
import com.raghunath.smartstore.dto.ShopResponse;
import com.raghunath.smartstore.service.ProductService;
import com.raghunath.smartstore.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/shops")
@RequiredArgsConstructor
@Slf4j
public class UserShopController {

    private final ShopService shopService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getShopsByCity(@RequestParam String city) {
        List<ShopListProjection> shops = shopService.getShopsByCity(city);

        // ✅ FIXED: Explicit typing solves generics issue
        List<Map<String, Object>> shopMaps = shops.stream()
                .map(shop -> {
                    Map<String, Object> shopMap = new HashMap<>();
                    shopMap.put("shopId", shop.getId());
                    shopMap.put("shopName", shop.getShopName());
                    shopMap.put("isApproved", shop.getIsApproved());
                    shopMap.put("rating", shop.getRating() != null ? shop.getRating() : 0.0);
                    shopMap.put("city", shop.getCity());
                    shopMap.put("openingTime", shop.getOpeningTime());
                    shopMap.put("closingTime", shop.getClosingTime());
                    shopMap.put("bannerUrl", shop.getBannerUrl());
                    shopMap.put("totalProducts", productService.getProductCountByShop(shop.getId()));
                    shopMap.put("isOpen", shop.getIsOpen());
                    return shopMap;
                })
                .collect(Collectors.toList());  // ✅ PERFECT - No more error!

        return ResponseEntity.ok(Map.of(
                "city", city,
                "totalShops", shopMaps.size(),
                "shops", shopMaps
        ));
    }


    @GetMapping("/{shopId}")
    public ResponseEntity<ShopResponse> getShopById(@PathVariable String shopId){

        log.info("User request: Get shop details for shopId={}", shopId);

        ShopResponse shop = shopService.getShopDetailsById(shopId);

        return ResponseEntity.ok(shop);
    }
}
