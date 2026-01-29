package com.viecinema.booking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboInfo {
    private Integer comboId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean isActive;
}