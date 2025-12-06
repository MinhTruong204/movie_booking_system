package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SeatInfo {
    private Integer seatId;
    private String seatLabel;      // A1, A2, B1...
    private String rowLabel;       // A, B, C...
    private Integer seatNumber;    // 1, 2, 3...
    private Integer seatTypeId;
    private String seatTypeName;
    private BigDecimal price;

    /**
     * Trạng thái ghế:
     * - available: Có thể đặt
     * - booked: Đã được đặt
     * - held: Đang được giữ bởi user khác
     * - held_by_you: Đang được giữ bởi current user
     * - disabled: Ghế không hoạt động
     */
    private String status;

    /**
     * Thời gian còn lại nếu ghế đang held (giây)
     * Null nếu không phải held
     */
    private Long holdExpiresIn;

    /**
     * Ghế có thể chọn được không
     */
    private Boolean isSelectable;
}

