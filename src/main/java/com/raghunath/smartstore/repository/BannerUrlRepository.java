package com.raghunath.smartstore.repository;

import com.raghunath.smartstore.entity.BannerUrl;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerUrlRepository extends MongoRepository<BannerUrl, String>{
    Optional<BannerUrl> findByShopType(String shopType);
}
