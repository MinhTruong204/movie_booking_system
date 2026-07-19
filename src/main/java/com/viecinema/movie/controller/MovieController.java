package com.viecinema.movie.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.enums.MovieStatus;
import com.viecinema.movie.dto.MovieDetail;
import com.viecinema.movie.dto.MovieSummary;
import com.viecinema.movie.dto.TopMovieDto;
import com.viecinema.movie.dto.request.MovieFilterRequest;
import com.viecinema.movie.dto.response.PagedResponse;
import com.viecinema.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.viecinema.common.constant.ApiConstant.*;

@RestController
@RequestMapping(MOVIE_PATH)
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Movies", description = "Browse movies that are currently showing or coming soon")
public class MovieController {
    private final MovieService movieService;

    @Operation(
            summary = "Get now-showing movies",
            description = "Returns a paginated list of movies currently showing in cinemas. Supports filtering by genre, keyword, and pagination."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    @SecurityRequirements
    @GetMapping(MOVIE_NOW_SHOWING_PATH)
    public ResponseEntity<ApiResponse<PagedResponse<MovieSummary>>>
    getNowShowingMovies(@Valid @ModelAttribute MovieFilterRequest request) {

        log.info("GET /api/movies/now-showing - Request: {}", request);
        PagedResponse<MovieSummary> movies = movieService.getMoviesByStatus(request, MovieStatus.NOW_SHOWING);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Movies now showing"));

    }

    @Operation(
            summary = "Get coming-soon movies",
            description = "Returns a paginated list of movies that are upcoming and not yet released."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    @SecurityRequirements
    @GetMapping(MOVIE_COMING_SOON_PATH)
    public ResponseEntity<ApiResponse<PagedResponse<MovieSummary>>>
    getComingSoonMovies(@Valid @ModelAttribute MovieFilterRequest request) {

        log.info("GET /api/movies/coming-soon");
        PagedResponse<MovieSummary> movies = movieService.getMoviesByStatus(request, MovieStatus.COMING_SOON);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Movies coming soon"));

    }

    @Operation(
            summary = "Get movie details",
            description = "Returns full details for a specific movie including cast, genres, and synopsis."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @SecurityRequirements
    @GetMapping(MOVIE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<MovieDetail>> getMovieDetail(
            @Parameter(description = "ID of the movie", required = true, example = "1")
            @PathVariable @Min(1) Integer movieId) {
        MovieDetail detail = movieService.getMovieDetail(movieId);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, detail, "Movie detail"));
    }

    // ========== MOVIE RANKING ENDPOINTS ==========

    @Operation(
            summary = "Get top-rated movies",
            description = "Returns movies with the highest average rating. Only includes movies with enough reviews to ensure reliability."
    )
    @SecurityRequirements
    @GetMapping(MOVIE_TOP_RATED_PATH)
    public ResponseEntity<ApiResponse<List<TopMovieDto>>> getTopRatedMovies(
            @Parameter(description = "Maximum number of movies to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @Parameter(description = "Minimum number of reviews required", example = "5")
            @RequestParam(defaultValue = "5") @Min(1) int minReviews) {

        log.info("GET /api/movies/top-rated - limit={}, minReviews={}", limit, minReviews);
        List<TopMovieDto> movies = movieService.getTopRatedMovies(limit, minReviews);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Top rated movies"));
    }

    @Operation(
            summary = "Get most-viewed movies",
            description = "Returns movies ranked by total number of bookings (viewers)."
    )
    @SecurityRequirements
    @GetMapping(MOVIE_MOST_VIEWED_PATH)
    public ResponseEntity<ApiResponse<List<TopMovieDto>>> getMostViewedMovies(
            @Parameter(description = "Maximum number of movies to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        log.info("GET /api/movies/most-viewed - limit={}", limit);
        List<TopMovieDto> movies = movieService.getMostViewedMovies(limit);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Most viewed movies"));
    }

    @Operation(
            summary = "Get outstanding movies",
            description = "Returns movies that simultaneously meet both a minimum average rating AND a minimum booking count threshold."
    )
    @SecurityRequirements
    @GetMapping(MOVIE_OUTSTANDING_PATH)
    public ResponseEntity<ApiResponse<List<TopMovieDto>>> getOutstandingMovies(
            @Parameter(description = "Maximum number of movies to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @Parameter(description = "Minimum average rating threshold (e.g. 4.0)", example = "4.0")
            @RequestParam(defaultValue = "4.0") double minRating,
            @Parameter(description = "Minimum number of bookings threshold (e.g. 100)", example = "100")
            @RequestParam(defaultValue = "100") @Min(0) int minBookings) {

        log.info("GET /api/movies/outstanding - limit={}, minRating={}, minBookings={}", limit, minRating, minBookings);
        List<TopMovieDto> movies = movieService.getOutstandingMovies(limit, minRating, minBookings);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Outstanding movies"));
    }
}




