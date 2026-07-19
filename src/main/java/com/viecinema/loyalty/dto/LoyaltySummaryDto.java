package com.viecinema.loyalty.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO tổng hợp thông tin điểm của user — dùng cho endpoint GET /my-points.
 */
@Data
@Builder
public class LoyaltySummaryDto {

    private Integer userId;

    /** Tổng điểm hiện tại */
    private Integer currentPoints;

    /** Tên hạng thành viên hiện tại */
    private String membershipTierName;

    /** Điểm cần để lên hạng tiếp theo (null nếu đã max) */
    private Integer pointsToNextTier;

    /** Tên hạng tiếp theo (null nếu đã max) */
    private String nextTierName;

    /** Màu sắc của tier hiện tại (#HEX) */
    private String tierColorCode;

    /** Tổng điểm đã tích (bao gồm cả đã dùng) */
    private long totalPointsEarned;

    /** Tổng điểm đã tiêu */
    private long totalPointsRedeemed;

    /** Tổng số tiền thực tế đã chi tiêu (chỉ tính booking PAID / CONFIRMED) */
    private BigDecimal totalSpent;
}
