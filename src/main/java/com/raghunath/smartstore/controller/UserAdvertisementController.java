package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.entity.Advertisement;
import com.raghunath.smartstore.service.AdvertisementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/advertisements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserAdvertisementController {

    private final AdvertisementService advertisementService;

    /**
     * Get active advertisements for a shop
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Advertisement>> getShopAdvertisements(
            @PathVariable String shopId) {
        return ResponseEntity.ok(advertisementService.getActiveAdvertisementsForShop(shopId));
    }

    /**
     * Get advertisement details by ID
     */
    @GetMapping("/{advertisementId}")
    public ResponseEntity<Advertisement> getAdvertisementDetails(
            @PathVariable String advertisementId) {
        return ResponseEntity.ok(advertisementService.getAdvertisementById(advertisementId));
    }
}
