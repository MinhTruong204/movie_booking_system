package com.viecinema.booking.service;

import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.HeldSeatDto;
import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.dto.response.HoldSeatsResponse;
import com.viecinema.booking.dto.UnavailableSeatDto;
import com.viecinema.booking.exception.SeatAlreadyHeldException;
import com.viecinema.booking.exception.SeatNotHeldByUserException;
import com.viecinema.common.exception.CustomBusinessException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.auth.entity.User;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.SeatStatus;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldingServiceImpl implements SeatHoldingService {

    private final SeatStatusRepository seatStatusRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;


    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public HoldSeatsResponse holdSeats(HoldSeatsRequest request, Integer userId) {

        // 1. Validate input
        validateRequest(request, userId);

        // 2. Verify showtime exists and not started
        Showtime showtime = validateShowtime(request.getShowtimeId());

        // Pre-fetch user to avoid fetching in loop
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId));

        // 3. Verify seats belong to the room
        validateSeatsInRoom(request.getSeatIds(), showtime.getRoom().getId());

        // 4. Lock và check seat status (PESSIMISTIC LOCK)
        List<SeatStatus> seatStatuses = seatStatusRepository
                .findByShowtimeAndSeatsWithLock(
                        request.getShowtimeId(),
                        request.getSeatIds()
                );

        // 5. Check availability
        List<UnavailableSeatDto> unavailableSeats = new ArrayList<>();
        List<Integer> availableSeats = new ArrayList<>();

        Map<Integer, SeatStatus> statusMap = seatStatuses.stream()
                .collect(Collectors.toMap(ss -> ss.getSeat().getSeatId(), Function.identity()));

        for (Integer seatId : request. getSeatIds()) {
            SeatStatus status = statusMap.get(seatId);

            if (status == null) {
                // Seat has no status record -> available for holding
                availableSeats.add(seatId);
            } else if (status.getStatus() == SeatStatus.Status.BOOKED) {
                // Seat is already booked
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat"));
                unavailableSeats.add(UnavailableSeatDto.builder()
                        .seatId(seatId)
                        .seatRow(seat.getSeatRow())
                        .seatNumber(seat.getSeatNumber())
                        .status(SeatStatus.Status.BOOKED.getValue())
                        .build());
            } else if (status.getStatus() == SeatStatus.Status.HELD) {
                // Check xem có phải user hiện tại đang giữ không
                if (! userId.equals(status.getHeldByUser().getId())) {
                    // Người khác đang giữ
                    Seat seat = seatRepository.findById(seatId)
                            .orElseThrow(() -> new ResourceNotFoundException("Seat"));
                    unavailableSeats. add(UnavailableSeatDto.builder()
                            . seatId(seatId)
                            .seatRow(seat.getSeatRow())
                            .seatNumber(seat. getSeatNumber())
                            . status(SeatStatus.Status.HELD.getValue())
                            . heldUntil(status.getHeldUntil())
                            . build());
                } else {
                    // Chính user này đang giữ -> extend time
                    availableSeats.add(seatId);
                }
            } else {
                // available
                availableSeats.add(seatId);
            }
        }

        // 6. Nếu có ghế unavailable -> throw exception
        if (! unavailableSeats.isEmpty()) {
            throw new SeatAlreadyHeldException(
                    "Some seats are unavailable",
                    unavailableSeats,
                    availableSeats
            );
        }

        // 7. Hold seats
        LocalDateTime heldUntil = LocalDateTime. now()
                .plusSeconds(request.getHoldDurationSeconds());

        List<HeldSeatDto> heldSeats = new ArrayList<>();
        List<Seat> seatsToHold = seatRepository.findAllById(request.getSeatIds());
        Map<Integer, Seat> seatMap = seatsToHold.stream()
                .collect(Collectors.toMap(Seat::getSeatId, Function.identity()));

        for (Integer seatId :  request.getSeatIds()) {
            SeatStatus status = statusMap.get(seatId);
            Seat seat = seatMap.get(seatId);
            if (seat == null) continue; // Should not happen due to validateSeatsInRoom

            if (status == null) {
                // Tạo mới
                status = SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatus.Status.HELD)
                        .heldByUser(user)
                        .heldUntil(heldUntil)
                        .build();
            } else {
                // Update
                status.setStatus(SeatStatus.Status.HELD);
                status.setHeldByUser(user);
                status.setHeldUntil(heldUntil);
            }

            seatStatusRepository.save(status);

            // Build response DTO
            heldSeats.add(HeldSeatDto.builder()
                    .seatId(seatId)
                    .seatRow(seat.getSeatRow())
                    .seatNumber(seat.getSeatNumber())
                    .seatType(seat.getSeatType().getName())
                    .heldUntil(heldUntil)
                    .build());
        }

        log.info("User {} held {} seats for showtime {}", userId, request.getSeatIds().size(), request.getShowtimeId());

        return HoldSeatsResponse.builder()
                .showtimeId(request.getShowtimeId())
                .heldSeats(heldSeats)
                .heldUntil(heldUntil)
                .remainingSeconds(request.getHoldDurationSeconds())
                .build();
    }

    @Override
    @Transactional
    public void releaseUserSeats(Integer userId) {
        int released = seatStatusRepository.releaseUserSeats(userId);
        log.info("Released {} seats for user {}", released, userId);
    }

    @Override
    @Transactional
    public int releaseExpiredSeats() {
        int released = seatStatusRepository.releaseExpiredSeats(LocalDateTime.now());
        if (released > 0) {
            log.info("Released {} expired seats", released);
        }
        return released;
    }

    // ========== VALIDATION METHODS ==========

    private void validateRequest(HoldSeatsRequest request, Integer userId) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new CustomBusinessException("No seats selected");
        }

        // Check user hold limit
        int currentHeld = seatStatusRepository.countHeldSeatsByUser(userId);
    }

    private Showtime validateShowtime(Integer showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime"));

        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new CustomBusinessException("Showtime has already started.");
        }

        if (! showtime.getIsActive() || showtime.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Showtime");
        }

        return showtime;
    }

    private void validateSeatsInRoom(List<Integer> seatIds, Integer roomId) {
        List<Seat> seats = seatRepository.findAllById(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new CustomBusinessException("Some seats don't exist.");
        }

        boolean allInRoom = seats.stream()
                .allMatch(seat -> seat.getRoom().getId().equals(roomId));

        if (!allInRoom) {
            throw new CustomBusinessException("Some seats don't belong to the room.");
        }
    }

    @Override
    @Transactional
    public void releaseSeat(Integer showtimeId, Integer seatId, Integer userId, boolean force) {
        // Optional: validate showtime exists (can skip if not necessary)
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new IllegalArgumentException("Showtime not found: " + showtimeId);
        }

        int updated;
        if (force) {
            updated = seatStatusRepository.forceReleaseSeat(showtimeId, seatId);
        } else {
            updated = seatStatusRepository.releaseSeatByUser(showtimeId, seatId, userId);
        }

        if (updated == 0) {
            // No row updated -> either seat not held, or held by someone else, or already released
            throw new SeatNotHeldByUserException("Seat not held by this user or already released");
        }

        log.info("Released seat {} for showtime {} by user {} (force={})", seatId, showtimeId, userId, force);
        // Optionally: publish an event or audit log here
    }
}