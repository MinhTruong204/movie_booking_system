package com.viecinema.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Nested class cho thống kê user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatistics {
    private BigDecimal totalSpent;
    private Integer totalBookings;
    private Integer totalTickets;
    private Integer totalReviews;
    private Integer watchlistCount;

    // Có thể thêm
    private Integer bookingsThisMonth;
    private BigDecimal spentThisMonth;
}
