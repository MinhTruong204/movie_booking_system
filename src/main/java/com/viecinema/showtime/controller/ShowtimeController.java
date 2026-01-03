package com.viecinema.showtime.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.SHOWTIMES_BY_MOVIE_PATH;
import static com.viecinema.common.constant.ApiConstant.SHOWTIMES_PATH;

@RestController
@RequestMapping(SHOWTIMES_PATH)
@RequiredArgsConstructor
@Validated
@Slf4j
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getShowtimes(@Valid ShowtimeFilterRequest request) {
        log.info("GET /api/showtimes");

        Object showtimes = showtimeService.findShowtimes(request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, showtimes, "Showtimes"));
    }

//    @GetMapping(SHOWTIMES_DETAIL_PATH)
//    public ResponseEntity<ApiResponse<ShowtimeDetailResponse>> getShowtimeDetail(
//            @PathVariable @Min(1) Integer showtimeId
//    ) {
//        log.info("GET /api/showtimes/{}", showtimeId);
//
//        ShowtimeDetailResponse showtimes = showtimeService.getShowtimeDetail(showtimeId);
//
//        return ResponseEntity.status(HttpStatus.OK).body(
//                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED,showtimes,"Showtimes detail"));
//    }

    @GetMapping(SHOWTIMES_BY_MOVIE_PATH)
    public ResponseEntity<ApiResponse<Object>> getShowtimesByMovie(
            @PathVariable Integer movieId,
            @Valid ShowtimeFilterRequest request) {
        log.info("GET /api/showtimes/by-movie/{}", movieId);

        request.setMovieId(movieId);
        Object showtimes = showtimeService.findShowtimes(request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, showtimes, "Showtimes"));
    }
}
