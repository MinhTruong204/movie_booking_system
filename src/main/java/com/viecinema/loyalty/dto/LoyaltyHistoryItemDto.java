package com.viecinema.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory.PointsType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO trả về một dòng lịch sử tích/tiêu điểm cho client.
 */
@Data
@Builder
public class LoyaltyHistoryItemDto {

    private Integer historyId;

    /** Số điểm thay đổi (dương = cộng, âm = trừ) */
    private Integer pointsChange;

    private PointsType pointsType;

    private String description;

    private Integer oldBalance;

    private Integer newBalance;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** Booking code liên quan (nếu là EARN) */
    private Integer bookingId;

    /** Review liên quan (nếu là BONUS review) */
    private Integer reviewId;
}
