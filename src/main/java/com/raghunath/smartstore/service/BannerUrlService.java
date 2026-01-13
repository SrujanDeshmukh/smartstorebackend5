package com.raghunath.smartstore.service;

import com.raghunath.smartstore.entity.BannerUrl;
import com.raghunath.smartstore.repository.BannerUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerUrlService {
    private final BannerUrlRepository bannerUrlRepository;
    private final Random random = new Random();

    public String getRandomBannerUrl(String shopType){
        BannerUrl bannerUrl = bannerUrlRepository.findByShopType(shopType)
                .orElseThrow(() -> new RuntimeException("No banner URLs found for shop type: " + shopType));

        List<String> urls = bannerUrl.getImageUrls();

        if(urls == null || urls.isEmpty()){
            throw new RuntimeException("No URLs available for shop type: " + shopType);
        }

        int randomIndex = random.nextInt(urls.size());
        String selectedUrl = urls.get(randomIndex);

        log.info("Selected random banner URL for shopType={}: {}", shopType, selectedUrl);

        return selectedUrl;
    }

    public String addBannerUrls(String shopType, List<String> imageUrls) {

        BannerUrl bannerUrl = new BannerUrl();
        bannerUrl.setShopType(shopType);
        bannerUrl.setImageUrls(imageUrls);

        bannerUrlRepository.save(bannerUrl);

        log.info("Banner URLs added for shopType={}", shopType);

        return "Banner URLs added successfully for " + shopType;
    }
}
