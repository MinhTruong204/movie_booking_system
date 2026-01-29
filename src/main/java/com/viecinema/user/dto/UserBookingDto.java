package com.viecinema.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.showtime.dto.CinemaInfo;
import com.viecinema.showtime.dto.MovieInfo;
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
public class UserBookingDto {

    // Booking basic info
    private Integer bookingId;
    private String bookingCode;
    private String status; // pending, paid, cancelled

    private BigDecimal totalAmount;
    private BigDecimal finalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // QR Code & Check-in
    private String qrCodeImageUrl;
    private Boolean isCheckedIn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm: ss")
    private LocalDateTime checkedInAt;

    // Showtime info
    private ShowtimeInfo showtime;

    // Movie info
    private MovieInfo movie;

    // Cinema & Room info
    private CinemaInfo cinema;

    // Seats
    private List<SeatInfo> seats;

    // Combos
    private List<BookingComboInfo> combos;

    // Payment info
    private PaymentInfo payment;

    // Helper flags cho UI
    private Boolean canCancel; // Có thể hủy vé không
    private Boolean canCheckIn; // Có thể check-in không
    private Boolean isUpcoming; // Suất chiếu sắp diễn ra
    private Boolean isPast; // Suất chiếu đã qua


}