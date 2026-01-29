package com.viecinema.booking.dto.response;

import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private ShowtimeInfo showtime;
    private List<SeatInfo> seats;
    private List<BookingComboInfo> combos;
    private PriceBreakdown priceBreakdown;
    private String qrCodeData;
    private String qrCodeImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
