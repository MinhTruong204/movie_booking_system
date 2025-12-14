package com.viecinema.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Integer bookingId;
    private String bookingCode;
    private String status;

    // Thông tin suất chiếu
    private ShowtimeInfo showtime;

    // Thông tin ghế
    private List<SeatInfo> seats;

    // Thông tin combo
    private List<ComboInfo> combos;

    // Thông tin giá
    private PriceBreakdown priceBreakdown;

    // QR Code (sau khi thanh toán)
    private String qrCodeData;
    private String qrCodeImageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // Thời hạn thanh toán

    @Data
    @Builder
    public static class ShowtimeInfo {
        private Integer showtimeId;
        private String movieTitle;
        private String cinemaName;
        private String roomName;
        private LocalDateTime startTime;
        private String posterUrl;
    }

    @Data
    @Builder
    public static class SeatInfo {
        private Integer seatId;
        private String seatRow;
        private Integer seatNumber;
        private String seatType;
        private BigDecimal price;
    }

    @Data
    @Builder
    public static class ComboInfo {
        private Integer comboId;
        private String comboName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    public static class PriceBreakdown {
        private BigDecimal ticketsSubtotal;    // Tổng tiền vé
        private BigDecimal combosSubtotal;     // Tổng tiền combo
        private BigDecimal subtotal;           // Tổng cộng trước giảm

        private BigDecimal promoDiscount;      // Giảm từ promo code
        private BigDecimal voucherDiscount;    // Giảm từ voucher
        private BigDecimal loyaltyDiscount;    // Giảm từ điểm
        private BigDecimal membershipDiscount; // Giảm từ hạng thành viên

        private BigDecimal totalDiscount;      // Tổng giảm
        private BigDecimal finalAmount;        // Số tiền phải trả

        // Điểm tích lũy nhận được
        private Integer pointsEarned;
    }
}
