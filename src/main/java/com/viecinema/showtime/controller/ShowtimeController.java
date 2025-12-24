package com.viecinema.showtime.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.service.ShowtimeService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.viecinema.common.constant.ApiConstant.*;

@RestController
@RequestMapping(SHOWTIMES_PATH)
@RequiredArgsConstructor
@Validated
@Slf4j
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getShowtimes(
            @RequestParam(required = false) @Min(1) Integer movieId,
            @RequestParam(required = false) @Min(1) Integer cinemaId,
            @RequestParam(required = false) @Min(1) Integer roomId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "CINEMA")
            ShowtimeFilterRequest.GroupBy groupBy,
            @RequestParam(required = false, defaultValue = "START_TIME")
            ShowtimeFilterRequest.SortBy sortBy,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            @RequestParam(required = false, defaultValue = "false") Boolean futureOnly,
            @RequestParam(required = false, defaultValue = "true") Boolean includeSeats
    ) {
        log.info("GET /api/showtimes - movieId={}, cinemaId={}, date={}, groupBy={}",
                movieId, cinemaId, date, groupBy);

        // Build filter request
        ShowtimeFilterRequest filter = ShowtimeFilterRequest.builder()
                .movieId(movieId)
                .cinemaId(cinemaId)
                .roomId(roomId)
                .date(date)
                .city(city)
                .groupBy(groupBy)
                .sortBy(sortBy)
                .activeOnly(activeOnly)
                .futureOnly(futureOnly)
                .includeAvailableSeats(includeSeats)
                .build();

        Object showtimes = showtimeService.findShowtimes(filter);

        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, showtimes,"Showtimes"));
    }

    @GetMapping(SHOWTIMES_DETAIL_PATH)
    public ResponseEntity<ApiResponse<ShowtimeDetailResponse>> getShowtimeDetail(
            @PathVariable @Min(1) Integer showtimeId
    ) {
        log.info("GET /api/showtimes/{}", showtimeId);

        ShowtimeDetailResponse showtimes = showtimeService.getShowtimeDetail(showtimeId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED,showtimes,"Showtimes detail"));
    }

    @GetMapping(SHOWTIMES_BY_MOVIE_PATH)
    public ResponseEntity<ApiResponse<Object>> getShowtimesByMovie(
            @PathVariable @Min(1) Integer movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String city
    ) {
        log.info("GET /api/showtimes/by-movie/{} - date={}, city={}", movieId, date, city);

        ShowtimeFilterRequest filter = ShowtimeFilterRequest.builder()
                .movieId(movieId)
                .date(date)
                .city(city)
                .groupBy(ShowtimeFilterRequest.GroupBy.CINEMA)
                .build();

        Object showtimes = showtimeService.findShowtimes(filter);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED,showtimes,"Showtimes"));
    }

    @GetMapping(SHOWTIMES_BY_CINEMA_PATH)
    public ResponseEntity<ApiResponse<Object>> getShowtimesByCinema(
            @PathVariable @Min(1) Integer cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat. ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "TIMESLOT")
            ShowtimeFilterRequest.GroupBy groupBy
    ) {
        log. info("GET /api/showtimes/by-cinema/{} - date={}, groupBy={}", cinemaId, date, groupBy);

        ShowtimeFilterRequest filter = ShowtimeFilterRequest. builder()
                .cinemaId(cinemaId)
                .date(date)
                .groupBy(groupBy)
                .build();

        Object showtimes = showtimeService.findShowtimes(filter);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED,showtimes,"Showtimes"));
    }
}
