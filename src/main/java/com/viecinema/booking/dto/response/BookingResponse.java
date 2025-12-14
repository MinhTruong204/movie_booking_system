package com.viecinema.booking.dto.response;

import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
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
    private BookingStatus status;

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
}
