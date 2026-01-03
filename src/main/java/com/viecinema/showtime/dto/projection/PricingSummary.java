package com.viecinema.showtime.dto.projection;

import java.math.BigDecimal;

public interface PricingSummary {
    String getSeatTypeName();

    BigDecimal getFinalPrice();

    Integer getAvailableCount();
}
