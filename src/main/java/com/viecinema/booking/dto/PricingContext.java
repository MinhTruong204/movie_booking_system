package com.viecinema.booking.dto;

import com.viecinema.auth.entity.User;
import com.viecinema.booking.entity.Combo;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.Showtime;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PricingContext {
    private User user;
    private Showtime showtime;
    private List<Seat> seats;
    private Map<Combo, Integer> selectedCombos;
    private String promoCode;
    private String voucherCode;
    private Integer useLoyaltyPoints;
}
