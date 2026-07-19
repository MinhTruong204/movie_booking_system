package com.viecinema.booking.dto.request;

import com.viecinema.booking.dto.SelectedCombo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    private List<SelectedCombo> combos;

    @Size(max = 50)
    private String promoCode;

    @Size(max = 50)
    private String voucherCode;


    /** ID voucher loại TICKET_DISCOUNT để giảm tiền vé. Chỉ được dùng 1 voucher loại này/booking. */
    private Integer ticketVoucherId;

    /** ID voucher loại COMBO_DISCOUNT để miễn phí combo. Chỉ được dùng 1 voucher loại này/booking. */
    private Integer comboVoucherId;
}