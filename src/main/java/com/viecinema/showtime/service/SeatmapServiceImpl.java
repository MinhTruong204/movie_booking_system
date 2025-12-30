package com.viecinema.showtime.service;

import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.entity.Movie;
import com.viecinema.showtime.dto.*;
import com.viecinema.showtime.dto.response.SeatmapResponse;
import com.viecinema.showtime.entity.*;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SeatmapServiceImpl implements SeatmapService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatStatusRepository seatStatusRepository;

    @Override
    public SeatmapResponse getSeatmap(Integer showtimeId, Integer currentUserId) {
        log.info("Fetching seatmap for showtime: {}, user: {}", showtimeId, currentUserId);

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime " + showtimeId));

        validateShowtime(showtime);

        Room room = showtime.getRoom();
        Movie movie = showtime.getMovie();
        BigDecimal basePrice = showtime.getBasePrice();

        List<Seat> seats = seatRepository.findByRoomIdOrderBySeatRowAndNumber(room.getId());
        Map<Integer,SeatStatus> seatStatusMap = getSeatStatusMap(showtimeId);

        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("Seats in roomId " + room.getId());
        }

        return buildSeatmapResponse(showtime, room, movie, seats, seatStatusMap, basePrice, currentUserId);
    }

    // ==================== PRIVATE METHODS ====================

    private void validateShowtime(Showtime showtime) {
        if (!showtime.getIsActive()) {
            throw new IllegalStateException("This showtime is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (showtime.getStartTime().isBefore(now)) {
            throw new IllegalStateException("This showtime has already started, tickets cannot be booked.");
        }

         if (showtime.getStartTime().minusMinutes(15).isBefore(now)) {
             throw new IllegalStateException("The booking deadline has passed.");
         }
    }

    private Map<Integer, SeatStatus> getSeatStatusMap(Integer showtimeId) {
        List<SeatStatus> seatStatuses = seatStatusRepository.findByShowtimeId(showtimeId);

        if (seatStatuses.isEmpty()) {
            return Collections.emptyMap();
        }

        return seatStatuses.stream()
                .collect(Collectors.toMap(ss -> ss.getSeat().getSeatId(), Function.identity()));
    }

    private SeatmapResponse buildSeatmapResponse(
            Showtime showtime,
            Room room,
            Movie movie,
            List<Seat> seats,
            Map<Integer, SeatStatus> seatStatusMap,
            BigDecimal basePrice,
            Integer currentUserId) {

        // Build ShowtimeInfo
        ShowtimeInfo showtimeInfo = buildShowtimeInfo(showtime, movie);

        // Build RoomInfo
        RoomInfo roomInfo = buildRoomInfo(room, seats);

        // Build SeatTypes với giá đã tính
        Map<Integer, SeatTypeInfo> seatTypeInfoMap = buildSeatTypeInfoMap(seats, basePrice, seatStatusMap);

        // Build SeatLayout
        SeatLayout seatLayout = buildSeatLayout(seats, seatStatusMap, basePrice, currentUserId);

        // Build Summary
        SeatSummary summary = buildSeatSummary(seats, seatStatusMap);

        return SeatmapResponse.builder()
                .showtimeInfo(showtimeInfo)
                .roomInfo(roomInfo)
                .seatTypes(new ArrayList<>(seatTypeInfoMap.values()))
                .seatLayout(seatLayout)
                .summary(summary)
                .build();
    }

    private ShowtimeInfo buildShowtimeInfo(Showtime showtime, Movie movie) {
        return ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .movieId(movie.getMovieId())
                .movieTitle(movie.getTitle())
                .posterUrl(movie.getPosterUrl())
                .duration(movie.getDuration())
                .ageRating(movie.getAgeRating())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .build();
    }

    private RoomInfo buildRoomInfo(Room room, List<Seat> seats) {
        Set<String> rows = seats.stream()
                .map(Seat::getSeatRow)
                .collect(Collectors.toSet());

        int maxSeatsPerRow = seats.stream()
                .collect(Collectors.groupingBy(Seat::getSeatRow, Collectors.counting()))
                .values().stream()
                .mapToInt(Long::intValue)
                .max()
                .orElse(0);

        return RoomInfo.builder()
                .roomId(room.getId())
                .roomName(room.getName())
                .cinemaId(room.getCinema().getId())
                .cinemaName(room.getCinema().getName())
                .cinemaAddress(room.getCinema().getAddress())
                .totalSeats(room.getTotalSeats())
                .totalRows(rows.size())
                .maxSeatsPerRow(maxSeatsPerRow)
                .build();
    }

    private Map<Integer, SeatTypeInfo> buildSeatTypeInfoMap(
            List<Seat> seats,
            BigDecimal basePrice,
            Map<Integer, SeatStatus> seatStatusMap) {

        Map<Integer, SeatTypeInfo> result = new LinkedHashMap<>();

        // Group seats by type và count available
        Map<Integer, List<Seat>> seatsByType = seats.stream()
                .collect(Collectors.groupingBy(s -> s.getSeatType().getSeatTypeId()));

        for (Map.Entry<Integer, List<Seat>> entry : seatsByType.entrySet()) {
            SeatType seatType = entry.getValue().get(0).getSeatType();

            // Count seat available
            long availableCount = entry.getValue().stream()
                    .filter(seat -> {
                        SeatStatus status = seatStatusMap.get(seat.getSeatId());
                        return status == null || status.isAvailable();
                    })
                    .count();

            result.put(seatType.getSeatTypeId(), SeatTypeInfo.builder()
                    .seatTypeId(seatType.getSeatTypeId())
                    .name(seatType.getName())
                    .description(seatType.getDescription())
                    .priceMultiplier(seatType.getPriceMultiplier())
                    .finalPrice(seatType.calculatePrice(basePrice))
                    .colorCode(seatType.getColorCode())
                    .availableCount((int) availableCount)
                    .build());
        }

        return result;
    }

    private SeatLayout buildSeatLayout(
            List<Seat> seats,
            Map<Integer, SeatStatus> seatStatusMap,
            BigDecimal basePrice,
            Integer currentUserId) {

        // Group seats by row
        Map<String, List<Seat>> seatsByRow = seats.stream()
                .collect(Collectors.groupingBy(
                        Seat::getSeatRow,
                        LinkedHashMap::new, // Keep order
                        Collectors.toList()
                ));

        List<SeatRow> rows = new ArrayList<>();
        int rowIndex = 0;

        for (Map.Entry<String, List<Seat>> entry : seatsByRow.entrySet()) {
            String rowLabel = entry.getKey();
            List<Seat> rowSeats = entry.getValue();

            // Sort by seat number
            rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));

            List<SeatInfo> seatInfoList = new ArrayList<>();
            for (Seat seat : rowSeats) {
                SeatStatus status = seatStatusMap.get(seat.getSeatId());
                seatInfoList.add(buildSeatInfo(seat, status, basePrice, currentUserId));
            }

            rows.add(SeatRow.builder()
                    .rowLabel(rowLabel)
                    .rowIndex(rowIndex++)
                    .seats(seatInfoList)
                    .build());
        }

        return SeatLayout.builder()
                .rows(rows)
                .screenPosition("top")
                .build();
    }

    private SeatInfo buildSeatInfo(
            Seat seat,
            SeatStatus seatStatus,
            BigDecimal basePrice,
            Integer currentUserId) {

        SeatType seatType = seat.getSeatType();
        BigDecimal price = seatType.calculatePrice(basePrice);

        // Xác định status
        String status;
        Long holdExpiresIn = null;
        boolean isSelectable;

        if (!seat.getIsActive()) {
            status = "disabled";
            isSelectable = false;
        } else if (seatStatus == null) {
            // Chưa có record -> available
            status = "available";
            isSelectable = true;
        } else {
            switch (seatStatus.getStatus()) {
                case AVAILABLE -> {
                    status = "available";
                    isSelectable = true;
                }
                case BOOKED -> {
                    status = "booked";
                    isSelectable = false;
                }
                case HELD -> {
                    // Check if this seat be hold by current user or not
                    if (seatStatus.isHeldBy(currentUserId)) {
                        status = "held_by_you";
                        holdExpiresIn = seatStatus.getRemainingHoldSeconds();
                        isSelectable = true;
                    } else if (seatStatus.isAvailable()) {
                        status = "available";
                        isSelectable = true;
                    } else {
                        status = "held";
                        isSelectable = false;
                    }
                }
                default -> {
                    status = "available";
                    isSelectable = true;
                }
            }
        }

        return SeatInfo.builder()
                .seatId(seat.getSeatId())
                .seatLabel(seat.getSeatLabel())
                .rowLabel(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .seatTypeId(seatType.getSeatTypeId())
                .seatTypeName(seatType.getName())
                .price(price)
                .status(status)
                .holdExpiresIn(holdExpiresIn)
                .isSelectable(isSelectable)
                .build();
    }

    private SeatSummary buildSeatSummary(List<Seat> seats, Map<Integer, SeatStatus> seatStatusMap) {
        int total = seats.size();
        int available = 0;
        int booked = 0;
        int held = 0;
        int disabled = 0;

        for (Seat seat : seats) {
            if (! seat.getIsActive()) {
                disabled++;
                continue;
            }

            SeatStatus status = seatStatusMap.get(seat.getSeatId());
            if (status == null || status.isAvailable()) {
                available++;
            } else {
                switch (status.getStatus()) {
                    case BOOKED -> booked++;
                    case HELD -> {
                        if (status.isAvailable()) {
                            available++;
                        } else {
                            held++;
                        }
                    }
                    default -> available++;
                }
            }
        }

        double occupancyRate = total > 0
                ? ((double) booked / (total - disabled)) * 100
                : 0;

        return SeatSummary.builder()
                .totalSeats(total)
                .availableSeats(available)
                .bookedSeats(booked)
                .heldSeats(held)
                .disabledSeats(disabled)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .build();
    }
}