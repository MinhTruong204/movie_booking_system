package com.viecinema.booking.dto.request;

import com.viecinema.booking.dto.ComboItem;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Showtime ID cannot be left blank.")
    private Integer showtimeId;

    @NotEmpty(message = "Seat IDs cannot be left blank.")
    @Size(min = 1, max = 10, message = "Number of seats must be between 1 and 10.")
    private List<Integer> seatIds;

    // Combo IDs với số lượng
    private List<ComboItem> combos;

    // Mã khuyến mãi (có thể null)
    @Size(max = 50)
    private String promoCode;

    // Voucher code (có thể null)
    @Size(max = 50)
    private String voucherCode;

    // Sử dụng điểm tích lũy (true/false)
    private Boolean useLoyaltyPoints = false;

    // Số điểm muốn dùng (nếu useLoyaltyPoints = true)
    @Min(0)
    private Integer loyaltyPointsToUse = 0;
}