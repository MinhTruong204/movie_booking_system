package com.viecinema.booking.validator;

import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.exception.SeatNotHeldByUserException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class SeatHoldingValidator {

    private final SeatRepository seatRepository;

    public void validateRequest(HoldSeatsRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new SpecificBusinessException("No seats selected");
        }
    }

    public void validateShowtime(Showtime showtime) {
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new SpecificBusinessException("Showtime has already started.");
        }

        if (!showtime.getIsActive() || showtime.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Showtime");
        }
    }

    public void validateSeatsInRoom(List<Integer> seatIds, Integer roomId) {
        List<Seat> seats = seatRepository.findAllById(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new SpecificBusinessException("Some seats don't exist.");
        }

        boolean allInRoom = seats.stream()
                .allMatch(seat -> seat.getRoom().getId().equals(roomId));

        if (!allInRoom) {
            throw new SpecificBusinessException("Some seats don't belong to the room.");
        }
    }
    
}
