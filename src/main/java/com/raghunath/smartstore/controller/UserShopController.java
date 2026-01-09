package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.ShopResponse;
import com.raghunath.smartstore.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/shops")
@RequiredArgsConstructor
@Slf4j
public class UserShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getShopsByCity(
            @RequestParam(name = "city") String city){
        log.info("User request: Get shops for city={}",city);

        List<ShopResponse> shops = shopService.getShopsByCity(city);

        return ResponseEntity.ok(Map.of(
                "city", city,
                "totalShops", shops.size(),
                "shops", shops
        ));
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<ShopResponse> getShopById(@PathVariable String shopId){

        log.info("User request: Get shop details for shopId={}", shopId);

        ShopResponse shop = shopService.getShopDetailsById(shopId);

        return ResponseEntity.ok(shop);
    }
}
