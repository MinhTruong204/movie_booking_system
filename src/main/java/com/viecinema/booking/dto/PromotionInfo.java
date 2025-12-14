package com.viecinema.booking.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PromotionInfo {
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
}
