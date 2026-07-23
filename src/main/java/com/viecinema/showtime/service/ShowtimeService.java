package com.viecinema.showtime.service;

import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.entity.Movie;
import com.viecinema.movie.repository.MovieRepository;
import com.viecinema.showtime.dto.PricingInfo;
import com.viecinema.showtime.dto.SeatAvailability;
import com.viecinema.showtime.dto.ShowtimeGroupByCinemaDto;
import com.viecinema.showtime.dto.ShowtimeGroupByTimeSlotDto;
import com.viecinema.showtime.dto.projection.PricingSummary;
import com.viecinema.showtime.dto.projection.SeatStatusCount;
import com.viecinema.showtime.dto.request.CreateShowtimeRequest;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.dto.request.UpdateShowtimeRequest;
import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import com.viecinema.showtime.entity.Room;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.SeatStatus;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.mapper.ShowtimeMapper;
import com.viecinema.showtime.repository.RoomRepository;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import com.viecinema.showtime.repository.ShowtimeSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeMapper showtimeMapper;

    public Object findShowtimes(ShowtimeFilterRequest request) {
        // Validate request
        if (!request.isValid()) {
            throw new BadRequestException("Must provide at least movieId or cinemaId");
        }
        // Route based on groupBy
        return switch (request.getGroupBy()) {
            case CINEMA -> findShowtimesGroupByCinema(request);
            case TIMESLOT -> findShowtimesGroupByTimeSlot(request);
            case ROOM -> null;
            case NONE -> findAllShowtime(request);
        };
    }

    public List<ShowtimeDetailResponse> findAllShowtime(ShowtimeFilterRequest request) {
        log.info("Finding showtimes with request: {}", request);

        // Query from a repository
        Specification<Showtime> spec = Specification
                .where(ShowtimeSpecifications.hasMovieId(request.getMovieId()))
                .and(ShowtimeSpecifications.hasCinemaId(request.getCinemaId()))
                .and(ShowtimeSpecifications.hasRoomId(request.getRoomId()))
                .and(ShowtimeSpecifications.hasDate(request.getDate()))
                .and(ShowtimeSpecifications.hasCity(request.getCity()))
                .and(ShowtimeSpecifications.hasActiveOnly(request.getActiveOnly()))
                .and(ShowtimeSpecifications.hasFutureOnly(request.getFutureOnly()));

        List<Showtime> showtimes = showtimeRepository.findAll(spec);
        // Map to DTO
        List<ShowtimeDetailResponse> responses = showtimeMapper.toResponseList(showtimes);

        responses.forEach(this::enrichWithSeatAvailability);

        // Load pricing info if needed
        if (Boolean.TRUE.equals(request.getIncludeAvailableSeats())) {
            responses.forEach(this::enrichWithPricingInfo);
        }

        log.info("Found {} showtimes", responses.size());
        return responses;
    }

    public List<ShowtimeGroupByCinemaDto> findShowtimesGroupByCinema(ShowtimeFilterRequest request) {
        List<ShowtimeDetailResponse> allShowtimes = findAllShowtime(request);
        // Group by cinema
        Map<Integer, List<ShowtimeDetailResponse>> groupedMap = allShowtimes.stream()
                .filter(st -> st.getCinema() != null && st.getCinema().getCinemaId() != null)
                .collect(Collectors.groupingBy(st -> st.getCinema().getCinemaId()));

        // Convert to DTO
        List<ShowtimeGroupByCinemaDto> result = groupedMap.values().stream()
                .map(showtimes -> {
                    ShowtimeDetailResponse first = showtimes.getFirst();

                    ShowtimeGroupByCinemaDto dto = ShowtimeGroupByCinemaDto.builder()
                            .cinemaId(first.getCinema().getCinemaId())
                            .cinemaName(first.getCinema().getName())
                            .address(first.getCinema().getAddress())
                            .city(first.getCinema().getCity())
                            .showtimes(showtimes)
                            .build();

                    dto.calculateStats();
                    return dto;
                })
                .sorted(Comparator.comparing(ShowtimeGroupByCinemaDto::getCinemaName))
                .toList();

        log.info("Grouped {} showtimes into {} cinemas", allShowtimes.size(), result.size());
        return result;
    }

    public List<ShowtimeGroupByTimeSlotDto> findShowtimesGroupByTimeSlot(ShowtimeFilterRequest request) {
        List<ShowtimeDetailResponse> allShowtimes = findAllShowtime(request);

        // Calculate time slot for each showtime
        allShowtimes.forEach(st -> st.setTimeSlot(st.calculateTimeSlot()));

        // Group by time slot
        Map<String, List<ShowtimeDetailResponse>> groupedMap = allShowtimes.stream()
                .collect(Collectors.groupingBy(ShowtimeDetailResponse::getTimeSlot));

        // Convert to DTO and sort by time order
        List<String> timeSlotOrder = Arrays.asList("MORNING", "AFTERNOON", "EVENING", "NIGHT");

        List<ShowtimeGroupByTimeSlotDto> result = timeSlotOrder.stream()
                .filter(groupedMap::containsKey)
                .map(timeSlot -> {
                    List<ShowtimeDetailResponse> showtimes = groupedMap.get(timeSlot);

                    ShowtimeGroupByTimeSlotDto dto = ShowtimeGroupByTimeSlotDto.builder()
                            .timeSlot(timeSlot)
                            .showtimes(showtimes)
                            .build();

                    dto.calculateStats();
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Grouped {} showtimes into {} time slots", allShowtimes.size(), result.size());
        return result;
    }

    // ==========================================
    // ADMIN CRUD METHODS
    // ==========================================

    @Transactional
    public ShowtimeDetailResponse createShowtime(CreateShowtimeRequest request) {
        log.info("Creating new showtime: {}", request);

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + request.getMovieId() + " not found"));

        Room room = roomRepository.findByIdAndDeletedAtIsNull(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room with ID " + request.getRoomId() + " not found"));

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (endTime == null) {
            int durationMinutes = movie.getDuration() != null ? movie.getDuration() : 120;
            endTime = startTime.plusMinutes(durationMinutes + 15);
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }

        if (showtimeRepository.existsOverlappingShowtime(request.getRoomId(), startTime, endTime, null)) {
            throw new BadRequestException("Showtime overlaps with an existing showtime in room " + room.getName());
        }

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setBasePrice(request.getBasePrice());
        showtime.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        Showtime savedShowtime = showtimeRepository.save(showtime);

        generateInitialSeatStatuses(savedShowtime, room.getId());

        ShowtimeDetailResponse response = showtimeMapper.toResponse(savedShowtime);
        enrichWithSeatAvailability(response);
        enrichWithPricingInfo(response);
        return response;
    }

    public ShowtimeDetailResponse getShowtimeById(Integer id) {
        log.info("Getting showtime by ID: {}", id);
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime with ID " + id + " not found"));

        ShowtimeDetailResponse response = showtimeMapper.toResponse(showtime);
        enrichWithSeatAvailability(response);
        enrichWithPricingInfo(response);
        return response;
    }

    @Transactional
    public ShowtimeDetailResponse updateShowtime(Integer id, UpdateShowtimeRequest request) {
        log.info("Updating showtime ID {}: {}", id, request);

        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime with ID " + id + " not found"));

        boolean hasBookedOrHeld = seatStatusRepository.existsByShowtimeIdAndStatus(id, SeatStatusType.BOOKED)
                || seatStatusRepository.existsByShowtimeIdAndStatus(id, SeatStatusType.HELD);

        if (hasBookedOrHeld) {
            if (!showtime.getMovie().getMovieId().equals(request.getMovieId())) {
                throw new BadRequestException("Cannot change movie for a showtime with booked/held seats");
            }
            if (!showtime.getRoom().getId().equals(request.getRoomId())) {
                throw new BadRequestException("Cannot change room for a showtime with booked/held seats");
            }
            if (!showtime.getStartTime().equals(request.getStartTime())) {
                throw new BadRequestException("Cannot change start time for a showtime with booked/held seats");
            }
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + request.getMovieId() + " not found"));

        Room room = roomRepository.findByIdAndDeletedAtIsNull(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room with ID " + request.getRoomId() + " not found"));

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (endTime == null) {
            int durationMinutes = movie.getDuration() != null ? movie.getDuration() : 120;
            endTime = startTime.plusMinutes(durationMinutes + 15);
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }

        if (showtimeRepository.existsOverlappingShowtime(request.getRoomId(), startTime, endTime, id)) {
            throw new BadRequestException("Showtime overlaps with an existing showtime in room " + room.getName());
        }

        boolean roomChanged = !showtime.getRoom().getId().equals(request.getRoomId());

        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setBasePrice(request.getBasePrice());
        if (request.getIsActive() != null) {
            showtime.setIsActive(request.getIsActive());
        }

        Showtime updatedShowtime = showtimeRepository.save(showtime);

        if (roomChanged) {
            seatStatusRepository.deleteByShowtimeId(id);
            generateInitialSeatStatuses(updatedShowtime, room.getId());
        }

        ShowtimeDetailResponse response = showtimeMapper.toResponse(updatedShowtime);
        enrichWithSeatAvailability(response);
        enrichWithPricingInfo(response);
        return response;
    }

    @Transactional
    public void deleteShowtime(Integer id) {
        log.info("Deleting showtime ID: {}", id);
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime with ID " + id + " not found"));

        boolean hasBookedSeats = seatStatusRepository.existsByShowtimeIdAndStatus(id, SeatStatusType.BOOKED);
        if (hasBookedSeats) {
            throw new BadRequestException("Cannot delete showtime that already has booked seats");
        }

        showtime.setIsActive(false);
        showtime.setDeletedAt(LocalDateTime.now());
        showtimeRepository.save(showtime);
    }

    private void generateInitialSeatStatuses(Showtime showtime, Integer roomId) {
        List<Seat> activeSeats = seatRepository.findByRoomIdOrderBySeatRowAndNumber(roomId);
        if (activeSeats.isEmpty()) {
            log.warn("No active seats found for room ID: {}", roomId);
            return;
        }

        List<SeatStatus> seatStatuses = activeSeats.stream()
                .map(seat -> SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatusType.AVAILABLE)
                        .build())
                .collect(Collectors.toList());

        seatStatusRepository.saveAll(seatStatuses);
        log.info("Created {} initial seat statuses for showtime ID: {}", seatStatuses.size(), showtime.getId());
    }

    private void enrichWithSeatAvailability(ShowtimeDetailResponse st) {
        SeatAvailability seatAvailability = new SeatAvailability();
        List<SeatStatusCount> seatStatus = seatStatusRepository.countByShowtimeIdGroupByStatus(st.getShowtimeId());
        Map<String, Integer> statusMap = seatStatus.stream()
                .collect(Collectors.toMap(SeatStatusCount::getStatus, SeatStatusCount::getCount));

        seatAvailability.setTotalSeats(st.getRoom() != null ? st.getRoom().getTotalSeats() : 0);
        seatAvailability.setAvailableSeats(statusMap.getOrDefault("AVAILABLE", 0));
        seatAvailability.setBookedSeats(statusMap.getOrDefault("BOOKED", 0));
        seatAvailability.setHeldSeats(statusMap.getOrDefault("HELD", 0));

        st.setSeatAvailability(seatAvailability);
    }

    private void enrichWithPricingInfo(ShowtimeDetailResponse dto) {
        List<PricingSummary> pricingData = showtimeRepository.findPricingInfoByShowtime(dto.getShowtimeId());

        if (pricingData.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> pricesBySeatType = new HashMap<>();
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        for (PricingSummary data : pricingData) {
            String seatTypeName = data.getSeatTypeName();
            BigDecimal price = data.getFinalPrice();

            pricesBySeatType.put(seatTypeName, price.setScale(0, RoundingMode.HALF_UP));

            if (minPrice == null || price.compareTo(minPrice) < 0) {
                minPrice = price;
            }
            if (maxPrice == null || price.compareTo(maxPrice) > 0) {
                maxPrice = price;
            }
        }

        dto.setPricing(PricingInfo.builder()
                .basePrice(dto.getBasePrice())
                .pricesBySeatType(pricesBySeatType)
                .minPrice(minPrice != null ? minPrice.setScale(0, RoundingMode.HALF_UP) : dto.getBasePrice())
                .maxPrice(maxPrice != null ? maxPrice.setScale(0, RoundingMode.HALF_UP) : dto.getBasePrice())
                .build());
    }
}
