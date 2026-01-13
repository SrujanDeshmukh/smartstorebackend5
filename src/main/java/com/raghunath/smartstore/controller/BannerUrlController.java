package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.service.BannerUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/banner")
@RequiredArgsConstructor
@Slf4j
public class BannerUrlController {
    private final BannerUrlService bannerUrlService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getRandomBannerUrl(@RequestParam(name = "shopType") String shopType) {
        log.info("Krishna request: Get random banner for shopType = {}", shopType);

        String bannerUrl = bannerUrlService.getRandomBannerUrl(shopType);

        return ResponseEntity.ok(Map.of(
                "shopType", shopType,
                "bannerUrl", bannerUrl
        ));
    }
}
