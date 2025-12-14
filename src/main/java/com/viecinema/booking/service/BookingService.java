package com.viecinema.booking.service;

import com.viecinema.booking.dto.ComboItem;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.booking.dto.request.BookingRequest;
import com.viecinema.booking.dto.response.BookingResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BookingService {
    @Transactional
    BookingResponse createBooking(Integer userId, BookingRequest request);

    /**
     * Kiểm tra tính khả dụng của ghế
     */
    void validateSeatAvailability(Integer showtimeId, List<Integer> seatIds);

    /**
     * Tính toán giá vé
     */
    PriceBreakdown calculatePrice(
            Integer userId,
            Integer showtimeId,
            List<Integer> seatIds,
            List<ComboItem> combos,
            String promoCode,
            String voucherCode,
            Integer loyaltyPointsToUse
    );
}
