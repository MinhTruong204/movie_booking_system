package com.viecinema.booking.service;

import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.dto.response.HoldSeatsResponse;

public interface SeatHoldingService {
    HoldSeatsResponse holdSeats(HoldSeatsRequest request, Integer userId);

    /**
     * Release tất cả ghế đang giữ của user
     */
    void releaseUserSeats(Integer userId);

    /**
     * Release ghế hết hạn (gọi bởi scheduler)
     */
    int releaseExpiredSeats();
    void releaseSeat(Integer showtimeId, Integer seatId, Integer userId, boolean force);
}
