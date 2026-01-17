// dto/ShopListProjection.java
package com.raghunath.smartstore.dto;

import java.time.LocalTime;

public interface ShopListProjection {
    String getId();
    String getShopName();
    String getCity();
    Boolean getIsApproved();
    Double getRating();
    LocalTime getOpeningTime();
    LocalTime getClosingTime();
    String getBannerUrl();
    Boolean getIsOpen();
}
