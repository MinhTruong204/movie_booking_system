package com.viecinema.booking.dto.request;

import com.viecinema.booking.dto.SelectedCombo;
import com.viecinema.common.validation.annotation.ValidEmail;
import com.viecinema.common.validation.annotation.ValidPhone;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class GuestBookingRequest {

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

    private Boolean useLoyaltyPoints = false;

    @Min(0)
    private Integer loyaltyPointsToUse = 0;


    @NotBlank
    @ValidEmail
    private String email;

    @ValidPhone
    private String phoneNumber;

    @Size(max = 50)
    @NotBlank(message = "Full name cannot be left blank.")
    private String fullName;
}
