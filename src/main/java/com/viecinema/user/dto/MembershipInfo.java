package com.viecinema.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nested class cho thông tin membership
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipInfo {
    private Integer tierId;
    private String tierName;
    private String tierDescription;
    private String colorCode;
    private String iconUrl;

    // Điểm tích lũy
    private Integer currentPoints;
    private Integer pointsRequired; // Điểm cần để lên hạng tiếp theo
    private Integer pointsToNextTier; // Điểm còn thiếu

    // Ưu đãi
    private BigDecimal discountPercent;
    private BigDecimal birthdayDiscount;
    private Integer freeTicketsPerYear;
    private Integer freeTicketsRemaining; // Số vé free còn lại trong năm
    private Boolean priorityBooking;

    // Thời gian
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate memberSince;

    // Progress bar data (để FE render thanh tiến trình)
    private Double progressPercent; // % tiến độ lên hạng: 0-100
}