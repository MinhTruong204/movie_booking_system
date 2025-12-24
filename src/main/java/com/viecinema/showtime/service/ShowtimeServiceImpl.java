package com.viecinema.showtime.service;

import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.showtime.dto.*;
import com.viecinema.showtime.dto.projection.PricingSummary;
import com.viecinema.showtime.dto.projection.SeatStatusCount;
import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.mapper.ShowtimeMapper;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import com.viecinema.showtime.repository.ShowtimeSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final ShowtimeMapper showtimeMapper;

    @Override
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

    @Override
    public List<ShowtimeDetailResponse> findAllShowtime(ShowtimeFilterRequest request) {
        log.info("Finding showtimes with request: {}", request);

        // Query from repository
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

        responses.forEach(st -> {
            SeatAvailability seatAvailability = new SeatAvailability();
            List<SeatStatusCount> seatStatus = seatStatusRepository.countByShowtimeIdGroupByStatus(st.getShowtimeId());
            Map<String, Integer> statusMap = seatStatus.stream()
                    .collect(Collectors.toMap(SeatStatusCount::getStatus, SeatStatusCount::getCount));

            seatAvailability.setTotalSeats(st.getRoom().getTotalSeats());
            seatAvailability.setAvailableSeats(statusMap.getOrDefault("AVAILABLE", 0));
            seatAvailability.setBookedSeats(statusMap.getOrDefault("BOOKED", 0));
            seatAvailability.setHeldSeats(statusMap.getOrDefault("HELD", 0));

            st.setSeatAvailability(seatAvailability);
        });
        // Load pricing info if needed
        if (request.getIncludeAvailableSeats()) {
            responses.forEach(this::enrichWithPricingInfo);
        }

        log.info("Found {} showtimes", responses.size());
        return responses;
    }

    @Override
    public List<ShowtimeGroupByCinemaDto> findShowtimesGroupByCinema(ShowtimeFilterRequest request) {
        List<ShowtimeDetailResponse> allShowtimes = findAllShowtime(request);
        // Group by cinema
        Map<Integer, List<ShowtimeDetailResponse>> groupedMap = allShowtimes.stream()
                .collect(Collectors.groupingBy(st -> st.getCinema().getCinemaId()));

        // Convert to DTO
        List<ShowtimeGroupByCinemaDto> result = groupedMap.entrySet().stream()
                .map(entry -> {
                    List<ShowtimeDetailResponse> showtimes = entry.getValue();
                    ShowtimeDetailResponse first = showtimes.get(0);

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

    @Override
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

    private void enrichWithPricingInfo(ShowtimeDetailResponse dto) {
        List<PricingSummary> pricingData = showtimeRepository.findPricingInfoByShowtime(dto.getShowtimeId());

        if (pricingData.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> pricesBySeatType = new HashMap<>();
        Map<String, Integer> availableSeatsByType = new HashMap<>();
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        for (PricingSummary data : pricingData) {
            String seatTypeName = data.getSeatTypeName();
            BigDecimal price = data.getFinalPrice();
            Integer availableCount = data.getAvailableCount();

            pricesBySeatType.put(seatTypeName, price. setScale(0, RoundingMode.HALF_UP));
            availableSeatsByType.put(seatTypeName, availableCount);

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
                .minPrice(minPrice.setScale(0, RoundingMode. HALF_UP))
                .maxPrice(maxPrice.setScale(0, RoundingMode.HALF_UP))
                .build());
    }
}
