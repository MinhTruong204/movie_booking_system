package com.viecinema.booking.service;

import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.showtime.repository.SeatStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.viecinema.common.constant.PolicyConstants.BOOKING_EXPIRATION_MINUTES;
import static com.viecinema.common.constant.PolicyConstants.SCHEDULER_DELAY_MS;

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
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.PENDING,
                expirationTime
        );

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired bookings. Processing cancellation...", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            // 1. Update Booking status to CANCELLED
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // 2. Release corresponding seats to AVAILABLE
            if (booking.getBookingSeats() != null && !booking.getBookingSeats().isEmpty()) {
                List<Integer> seatIds = booking.getBookingSeats().stream()
                        .map(bs -> bs.getSeat().getSeatId())
                        .collect(Collectors.toList());
                int releasedRows = seatStatusRepository.releaseSeats(
                        booking.getShowtime().getId(),
                        seatIds,
                        SeatStatusType.AVAILABLE
                );
                log.info("Released {} seats for expired booking {}", releasedRows, booking.getBookingCode());
            }
        }
    }

    @Scheduled(fixedDelay = SCHEDULER_DELAY_MS)
    @Transactional
    public void releaseHeldSeats() {
        int updatedRows = seatStatusRepository.updateSeatStatusForExpiredHolding(
                SeatStatusType.AVAILABLE,
                SeatStatusType.HELD);
        log.info("Found {} expired holding seats. Processing release...", updatedRows);
    }
}
