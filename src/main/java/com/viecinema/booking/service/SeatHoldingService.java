package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.HeldSeatDto;
import com.viecinema.booking.dto.UnavailableSeatDto;
import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.dto.response.HoldSeatsResponse;
import com.viecinema.booking.exception.SeatAlreadyHeldException;
import com.viecinema.booking.exception.SeatNotHeldByUserException;
import com.viecinema.booking.validator.SeatHoldingValidator;
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
public class SeatHoldingService {

    private final SeatStatusRepository seatStatusRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatHoldingValidator seatHoldingValidator;



    @Transactional
    public HoldSeatsResponse holdSeats(HoldSeatsRequest request, Integer userId) {


        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId));

        //Verifying user, showtime, and seats
        seatHoldingValidator.validateRequest(request);
        seatHoldingValidator.validateShowtime(showtime);
        seatHoldingValidator.validateSeatsInRoom(request.getSeatIds(), showtime.getRoom().getId());

        // Lock and check seat status (PESSIMISTIC LOCK)
        List<SeatStatus> seatStatuses = seatStatusRepository
                .findByShowtimeAndSeatsWithLock(
                        request.getShowtimeId(),
                        request.getSeatIds()
                );

        // Check availability
        List<UnavailableSeatDto> unavailableSeats = new ArrayList<>();
        List<Integer> availableSeats = new ArrayList<>();
        Map<Integer, SeatStatus> statusMap = seatStatuses.stream()
                .collect(Collectors.toMap(ss -> ss.getSeat().getSeatId(), Function.identity()));

        for (Integer seatId : request.getSeatIds()) {
            SeatStatus status = statusMap.get(seatId);

            // Seat has no status record -> available for holding
            if (status == null) {
                availableSeats.add(seatId);
            }
            // Seat is already held by another user
            else if (status.getStatus() == SeatStatusType.BOOKED) {
                // Seat is already booked
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat"));
                unavailableSeats.add(UnavailableSeatDto.builder()
                        .seatId(seatId)
                        .seatRow(seat.getSeatRow())
                        .seatNumber(seat.getSeatNumber())
                        .status(SeatStatusType.BOOKED.getValue())
                        .build());
            }
            // One user already holds a seat
            else if (status.getStatus() == SeatStatusType.HELD) {
                // Seat is already held by another user
                if (!userId.equals(status.getHeldByUser().getId())) {
                    Seat seat = seatRepository.findById(seatId)
                            .orElseThrow(() -> new ResourceNotFoundException("Seat"));
                    unavailableSeats.add(UnavailableSeatDto.builder()
                            .seatId(seatId)
                            .seatRow(seat.getSeatRow())
                            .seatNumber(seat.getSeatNumber())
                            .status(SeatStatusType.HELD.getValue())
                            .heldUntil(status.getHeldUntil())
                            .build());
                } else {
                    // Seat is already held by the current user
                    availableSeats.add(seatId);
                }
            } else {
                // available
                availableSeats.add(seatId);
            }
        }

        if (!unavailableSeats.isEmpty()) {
            throw new SeatAlreadyHeldException(
                    "Some seats are unavailable",
                    unavailableSeats,
                    availableSeats
            );
        }

        // Hold seats
        LocalDateTime heldUntil = LocalDateTime.now().plusSeconds(request.getHoldDurationSeconds());
        List<HeldSeatDto> heldSeats = new ArrayList<>();
        List<Seat> seatsToHold = seatRepository.findAllById(request.getSeatIds());
        Map<Integer, Seat> seatMap = seatsToHold.stream()
                .collect(Collectors.toMap(Seat::getSeatId, Function.identity()));

        // Update status for each seat
        for (Integer seatId : request.getSeatIds()) {
            SeatStatus status = statusMap.get(seatId);
            Seat seat = seatMap.get(seatId);

            if (status == null) {
                // Create
                status = SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatusType.HELD)
                        .heldByUser(user)
                        .heldUntil(heldUntil)
                        .build();
            } else {
                // Update
                status.setStatus(SeatStatusType.HELD);
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

    @Transactional
    public void releaseUserSeats(Integer userId) {
        int released = seatStatusRepository.releaseUserSeats(userId);
        log.info("Released {} seats for user {}", released, userId);
    }

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