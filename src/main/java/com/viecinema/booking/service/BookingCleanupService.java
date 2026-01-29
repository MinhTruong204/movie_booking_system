package com.viecinema.booking.service;

import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.showtime.entity.SeatStatus;
import com.viecinema.showtime.repository.SeatStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.viecinema.common.constant.PolicyConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCleanupService {

    private final BookingRepository bookingRepository;
    private final SeatStatusRepository seatStatusRepository;

    @Scheduled(fixedDelay = SCHEDULER_DELAY_MS)
    @Transactional
    public void cancelUnpaidBookings() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(BOOKING_EXPIRATION_MINUTES);
        int updatedRows = bookingRepository.updateStatusForExpiredBookings(
                BookingStatus.CANCELLED,
                BookingStatus.PENDING,
                expirationTime
        );

        log.info("Found {} expired bookings. Processing cancellation...", updatedRows);
    }

    @Scheduled(fixedDelay = SCHEDULER_DELAY_MS)
    @Transactional
    public void releaseHeldSeats() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(SEAT_HOLDING_MINUTES);
        int updatedRows = seatStatusRepository.updateSeatStatusForExpiredHolding(
                SeatStatusType.AVAILABLE,
                SeatStatusType.HELD,
                expirationTime);
        log.info("Found {} expired holding seats. Processing release...", updatedRows);
    }
}
