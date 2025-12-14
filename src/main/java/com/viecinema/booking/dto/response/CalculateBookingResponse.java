package com.viecinema.booking.dto.response;

import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateBookingResponse {

    private ShowtimeInfo showtime;
    private List<SeatInfo> seats;
    private List<ComboInfo> combos;
    private PriceBreakdown pricingBreakdown;
}
