package com.viecinema.booking.validator;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.ComboItemSelected;
import com.viecinema.booking.entity.Combo;
import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.SeatStatus;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingValidator {
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatStatusRepository seatStatusRepository;

    public void validateUser(User user) {
        if (!user.getIsActive()) {
            throw new SpecificBusinessException("The account has been locked.");
        }
    }
    public void validateShowtime(Showtime showtime) {
        if (!showtime.getIsActive()) {
            throw new SpecificBusinessException("The showtime is not available.");
        }

        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new SpecificBusinessException("The showtime has already started.");
        }

        // Ticket reservations are not accepted less than 15 minutes before showtime.
        if (showtime.getStartTime().minusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new SpecificBusinessException("Tickets cannot be booked for the upcoming screening.");
        }
    }

    public void validateSeatAvailability(Integer showtimeId, List<Integer> seatIds, Integer userId) {
        // Get seat statuses
        List<SeatStatus> seatStatuses = seatStatusRepository
                .findByShowtimeAndSeatsWithLock(showtimeId, seatIds);

        // Check seat statuses
        if (seatStatuses.size() != seatIds.size()) {
            Set<Integer> existingSeatIds = seatStatuses.stream()
                    .map(ss -> ss.getSeat().getSeatId())
                    .collect(Collectors.toSet());
            // Get missing seat ids, which one is not in existingSeatIds
            List<Integer> missingSeatIds = seatIds.stream()
                    .filter(id -> !existingSeatIds.contains(id))
                    .collect(Collectors.toList());

            List<Seat> missingSeats = seatRepository.findAllById(missingSeatIds);
            Showtime showtime = showtimeRepository.findById(showtimeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

            // Save a new status for missing seats
            for (Seat seat : missingSeats) {
                SeatStatus newStatus = SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatusType.AVAILABLE)
                        .version(0)
                        .build();
                seatStatusRepository.save(newStatus);
            }

            // Get all seat statuses again
            seatStatuses = seatStatusRepository
                    .findByShowtimeAndSeatsWithLock(showtimeId, seatIds);
        }

        // Check status
        LocalDateTime now = LocalDateTime.now();
        for (SeatStatus status : seatStatuses) {
            if (SeatStatusType.BOOKED.equals(status.getStatus())) {
                throw new SpecificBusinessException(
                        String.format("Seat %s%d has been reserved",
                                status.getSeat().getSeatRow(),
                                status.getSeat().getSeatNumber())
                );
            }

            if (SeatStatusType.HELD.equals(status.getStatus())) {
                // Kiểm tra xem ghế có hết hạn giữ chưa
                if (status.getHeldUntil() != null && status.getHeldUntil().isAfter(now)) {
                    // Nếu ghế đang được giữ bởi người khác, báo lỗi
                    if (status.getHeldByUser() != null && !status.getHeldByUser().getId().equals(userId)) {
                        throw new SpecificBusinessException(
                                String.format("Seat %s%d is held by another user",
                                        status.getSeat().getSeatRow(),
                                        status.getSeat().getSeatNumber())
                        );
                    }
                }
            }
        }
    }

    public void validateCombo(List<Integer> comboIds, List<Combo> combos) {
        if (combos.size() != comboIds.size()) {
            throw new SpecificBusinessException("Một số combo không tồn tại");
        }

        for (Combo combo : combos) {
            if (!combo.getIsActive()) {
                throw new SpecificBusinessException("Combo " + combo.getName() + " không khả dụng");
            }
        }
    }
}
