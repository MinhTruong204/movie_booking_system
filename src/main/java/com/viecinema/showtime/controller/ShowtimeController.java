package com.viecinema.showtime.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Showtimes", description = "Query available showtimes with optional filters")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @Operation(
            summary = "Get all showtimes",
            description = "Returns showtimes optionally filtered by cinema, date, or movie. When grouped by date, the response is a map of date -> list of showtimes."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtimes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getShowtimes(@Valid ShowtimeFilterRequest request) {
        log.info("GET /api/showtimes");

        Object showtimes = showtimeService.findShowtimes(request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, showtimes, "Showtimes"));
    }

    @Operation(
            summary = "Get showtimes by movie",
            description = "Returns all available showtimes for a specific movie, optionally filtered by date or cinema."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtimes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @SecurityRequirements
    @GetMapping(SHOWTIMES_BY_MOVIE_PATH)
    public ResponseEntity<ApiResponse<Object>> getShowtimesByMovie(
            @Parameter(description = "ID of the movie", required = true, example = "1")
            @PathVariable Integer movieId,
            @Valid ShowtimeFilterRequest request) {
        log.info("GET /api/showtimes/by-movie/{}", movieId);

        request.setMovieId(movieId);
        Object showtimes = showtimeService.findShowtimes(request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, showtimes, "Showtimes"));
    }
}


