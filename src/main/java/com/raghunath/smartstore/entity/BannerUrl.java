package com.raghunath.smartstore.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "banner_urls")
public class BannerUrl {

    @Id
    private String id;

    private String shopType;

    private List<String> imageUrls;
}
