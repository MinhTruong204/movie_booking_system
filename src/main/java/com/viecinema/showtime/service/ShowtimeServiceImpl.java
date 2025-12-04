package com.viecinema.showtime.service;

import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.showtime.dto.ShowtimeDetailResponse;
import com.viecinema.showtime.dto.ShowtimeFilterRequest;
import com.viecinema.showtime.dto.ShowtimeGroupByCinemaDto;
import com.viecinema.showtime.dto.ShowtimeGroupByTimeSlotDto;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    @Override
    public Object findShowtimes(ShowtimeFilterRequest request) {
        // Validate request
        if (! request.isValid()) {
            throw new BadRequestException("Must provide at least movieId or cinemaId");
        }

        // Route theo groupBy
        return switch (request.getGroupBy()) {
            case CINEMA -> findShowtimesGroupByCinema(request);
            case TIMESLOT -> findShowtimesGroupByTimeSlot(request);
            case ROOM -> findShowtimesGroupByRoom(request);
            case NONE -> findShowtimesList(request);
        };
    }

    @Override
    @Cacheable(value = "showtimes", key = "#request.toString()", unless = "#result. isEmpty()")
    public List<ShowtimeDetailResponse> findShowtimesList(ShowtimeFilterRequest request) {
        log.info("Finding showtimes with request: {}", request);

        // Query từ repository
        List<Object[]> rawResults = showtimeRepository.findShowtimesWithDetails(
                request.getMovieId(),
                request.getCinemaId(),
                request.getRoomId(),
                request.getCity(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getActiveOnly(),
                request.getFutureOnly(),
                request.getSortBy().name()
        );

        // Map sang DTO
        List<ShowtimeDetailResponse> showtimes = rawResults.stream()
                .map(this::mapToShowtimeDetailResponse)
                .collect(Collectors.toList());

        // Load pricing info nếu cần
        if (request.getIncludeAvailableSeats()) {
            showtimes.forEach(this::enrichWithPricingInfo);
        }

        log.info("Found {} showtimes", showtimes. size());
        return showtimes;
    }

    @Override
    public List<ShowtimeGroupByCinemaDto> findShowtimesGroupByCinema(ShowtimeFilterRequest request) {
        List<ShowtimeDetailResponse> allShowtimes = findShowtimesList(request);

        // Group by cinema
        Map<Integer, List<ShowtimeDetailResponse>> groupedMap = allShowtimes.stream()
                .collect(Collectors. groupingBy(
                        st -> st.getCinema().getCinemaId()
                ));

        // Convert to DTO
        List<ShowtimeGroupByCinemaDto> result = groupedMap.entrySet().stream()
                .map(entry -> {
                    List<ShowtimeDetailResponse> showtimes = entry.getValue();
                    ShowtimeDetailResponse first = showtimes.get(0);

                    ShowtimeGroupByCinemaDto dto = ShowtimeGroupByCinemaDto.builder()
                            .cinemaId(first.getCinema().getCinemaId())
                            .cinemaName(first. getCinema().getName())
                            .address(first.getCinema().getAddress())
                            .city(first.getCinema().getCity())
                            .showtimes(showtimes)
                            .build();

                    dto.calculateStats();
                    return dto;
                })
                .sorted(Comparator.comparing(ShowtimeGroupByCinemaDto::getCinemaName))
                .collect(Collectors.toList());

        log.info("Grouped {} showtimes into {} cinemas", allShowtimes.size(), result.size());
        return result;
    }

    @Override
    public List<ShowtimeGroupByTimeSlotDto> findShowtimesGroupByTimeSlot(ShowtimeFilterRequest request) {
        List<ShowtimeDetailResponse> allShowtimes = findShowtimesList(request);

        // Calculate time slot cho mỗi showtime
        allShowtimes.forEach(st -> st.setTimeSlot(st.calculateTimeSlot()));

        // Group by time slot
        Map<String, List<ShowtimeDetailResponse>> groupedMap = allShowtimes.stream()
                .collect(Collectors. groupingBy(ShowtimeDetailResponse::getTimeSlot));

        // Convert to DTO và sort theo thứ tự thời gian
        List<String> timeSlotOrder = Arrays.asList("MORNING", "AFTERNOON", "EVENING", "NIGHT");

        List<ShowtimeGroupByTimeSlotDto> result = timeSlotOrder.stream()
                .filter(groupedMap::containsKey)
                .map(timeSlot -> {
                    List<ShowtimeDetailResponse> showtimes = groupedMap.get(timeSlot);

                    ShowtimeGroupByTimeSlotDto dto = ShowtimeGroupByTimeSlotDto.builder()
                            .timeSlot(timeSlot)
                            . showtimes(showtimes)
                            .build();

                    dto.calculateStats();
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Grouped {} showtimes into {} time slots", allShowtimes.size(), result.size());
        return result;
    }

    /**
     * Group by room (bonus)
     */
    private List<Object> findShowtimesGroupByRoom(ShowtimeFilterRequest request) {
        List<ShowtimeDetailResponse> allShowtimes = findShowtimesList(request);

        // Group by cinema -> room
        Map<Integer, Map<Integer, List<ShowtimeDetailResponse>>> groupedMap = allShowtimes.stream()
                .collect(Collectors. groupingBy(
                        st -> st.getCinema().getCinemaId(),
                        Collectors. groupingBy(st -> st.getRoom().getRoomId())
                ));

        // Convert to nested structure
        List<Object> result = new ArrayList<>();
        groupedMap.forEach((cinemaId, roomsMap) -> {
            Map<String, Object> cinemaGroup = new HashMap<>();
            ShowtimeDetailResponse sampleShowtime = allShowtimes. stream()
                    .filter(st -> st.getCinema().getCinemaId().equals(cinemaId))
                    .findFirst()
                    .orElse(null);

            if (sampleShowtime != null) {
                cinemaGroup.put("cinemaId", cinemaId);
                cinemaGroup.put("cinemaName", sampleShowtime.getCinema().getName());

                List<Object> rooms = new ArrayList<>();
                roomsMap.forEach((roomId, showtimes) -> {
                    Map<String, Object> roomGroup = new HashMap<>();
                    roomGroup.put("roomId", roomId);
                    roomGroup. put("roomName", showtimes.get(0).getRoom().getName());
                    roomGroup.put("showtimes", showtimes);
                    rooms.add(roomGroup);
                });

                cinemaGroup.put("rooms", rooms);
                result.add(cinemaGroup);
            }
        });

        return result;
    }

    @Override
    public ShowtimeDetailResponse getShowtimeDetail(Integer showtimeId) {
        if (! showtimeRepository.existsByIdAndActive(showtimeId)) {
            throw new ResourceNotFoundException("Cannot find showtime with id: " + showtimeId);
        }

        ShowtimeFilterRequest request = ShowtimeFilterRequest.builder()
                .activeOnly(true)
                .futureOnly(false)
                . includeAvailableSeats(true)
                .build();

        List<Object[]> results = showtimeRepository.findShowtimesWithDetails(
                null, null, null, null,
                LocalDateTime.now(). minusDays(1),
                LocalDateTime.now(). plusDays(30),
                true, false, "START_TIME"
        );

        ShowtimeDetailResponse dto = results.stream()
                .map(this::mapToShowtimeDetailResponse)
                .filter(st -> st.getShowtimeId().equals(showtimeId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Can not find showtime"));

        enrichWithPricingInfo(dto);
        dto.setTimeSlot(dto.calculateTimeSlot());

        return dto;
    }

    /**
     * Map Object[] từ native query sang ShowtimeDetailResponse
     */
    private ShowtimeDetailResponse mapToShowtimeDetailResponse(Object[] row) {
        int idx = 0;

        ShowtimeDetailResponse dto = ShowtimeDetailResponse.builder()
                .showtimeId((Integer) row[idx++])
                .movie(ShowtimeDetailResponse.MovieInfo.builder()
                        .movieId((Integer) row[idx++])
                        .build())
                .room(ShowtimeDetailResponse.RoomInfo.builder()
                        . roomId((Integer) row[idx++])
                        .build())
                .startTime(((Timestamp) row[idx++]). toLocalDateTime())
                .endTime(((Timestamp) row[idx++]).toLocalDateTime())
                .basePrice((BigDecimal) row[idx++])
                .isActive((Boolean) row[idx++])
                .build();

        // Movie info
        dto.getMovie().setTitle((String) row[idx++]);
        dto.getMovie().setPosterUrl((String) row[idx++]);
        dto.getMovie().setDuration((Integer) row[idx++]);
        dto.getMovie().setAgeRating((String) row[idx++]);

        // Cinema info
        dto.setCinema(ShowtimeDetailResponse.CinemaInfo.builder()
                .cinemaId((Integer) row[idx++])
                .name((String) row[idx++])
                .address((String) row[idx++])
                .city((String) row[idx++])
                .build());

        // Room info
        dto.getRoom().setName((String) row[idx++]);
        dto.getRoom().setTotalSeats((Integer) row[idx++]);

        // Seat availability
        Integer totalSeats = ((Number) row[idx++]).intValue();
        Integer availableSeats = ((Number) row[idx++]).intValue();
        Integer bookedSeats = ((Number) row[idx++]).intValue();
        Integer heldSeats = ((Number) row[idx++]).intValue();

        double occupancyRate = totalSeats > 0 ?
                ((double) bookedSeats / totalSeats) * 100 : 0;

        dto.setSeatAvailability(ShowtimeDetailResponse.SeatAvailability.builder()
                . totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .bookedSeats(bookedSeats)
                .heldSeats(heldSeats)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .build());

        return dto;
    }

    /**
     * Enrich DTO with pricing info by seat type
     */
    private void enrichWithPricingInfo(ShowtimeDetailResponse dto) {
        List<Object[]> pricingData = showtimeRepository.findPricingInfoByShowtime(dto.getShowtimeId());

        if (pricingData.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> pricesBySeatType = new HashMap<>();
        Map<String, Integer> availableSeatsByType = new HashMap<>();
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        for (Object[] row : pricingData) {
            String seatTypeName = (String) row[0];
            BigDecimal price = (BigDecimal) row[1];
            Integer availableCount = ((Number) row[2]).intValue();

            pricesBySeatType.put(seatTypeName, price. setScale(0, RoundingMode.HALF_UP));
            availableSeatsByType.put(seatTypeName, availableCount);

            if (minPrice == null || price.compareTo(minPrice) < 0) {
                minPrice = price;
            }
            if (maxPrice == null || price.compareTo(maxPrice) > 0) {
                maxPrice = price;
            }
        }

        dto.setPricing(ShowtimeDetailResponse.PricingInfo.builder()
                .basePrice(dto.getBasePrice())
                .pricesBySeatType(pricesBySeatType)
                .minPrice(minPrice != null ? minPrice.setScale(0, RoundingMode. HALF_UP) : dto.getBasePrice())
                .maxPrice(maxPrice != null ?  maxPrice.setScale(0, RoundingMode.HALF_UP) : dto.getBasePrice())
                .build());

        if (dto.getSeatAvailability() != null) {
            dto.getSeatAvailability().setAvailableSeatsByType(availableSeatsByType);
        }
    }
}