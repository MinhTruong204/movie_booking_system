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

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCleanupService {

    private final BookingRepository bookingRepository;
    private final SeatStatusRepository seatStatusRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelUnpaidBookings() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);

        List<Booking> expiredBookings = bookingRepository.findAllByStatusAndCreatedAtBefore(
                BookingStatus.PENDING,
                expirationTime
        );

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired bookings. Processing cancellation...", expiredBookings.size());

        // Update booking status to CANCELLED
        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.CANCELLED);

            log.info("Auto-cancelling booking ID: {} - Created at: {}",
                    booking.getId(), booking.getCreatedAt());
        }
        bookingRepository.saveAll(expiredBookings);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseHeldSeats() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);

        List<SeatStatus> expiredSeats = seatStatusRepository.findAllByStatusAndCreatedAtBefore(
                SeatStatusType.HELD, expirationTime);

        if (!expiredSeats.isEmpty()) {
            log.info("Found {} expired held seats. Releasing...", expiredSeats.size());
            seatStatusRepository.deleteAll(expiredSeats);
        }
    }
}
