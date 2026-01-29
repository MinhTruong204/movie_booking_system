package com.viecinema.booking.dto.response;

import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateBookingResponse {
    private ShowtimeInfo showtime;
    private List<SeatInfo> seats;
    private List<BookingComboInfo> combos;
    private PriceBreakdown pricingBreakdown;
}
