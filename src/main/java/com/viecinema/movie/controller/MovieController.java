package com.viecinema.movie.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.enums.MovieStatus;
import com.viecinema.movie.dto.MovieDetail;
import com.viecinema.movie.dto.MovieSummary;
import com.viecinema.movie.dto.request.MovieFilterRequest;
import com.viecinema.movie.dto.response.PagedResponse;
import com.viecinema.movie.service.MovieService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.viecinema.common.constant.ApiConstant.*;

@RestController
@RequestMapping(MOVIE_PATH)
@RequiredArgsConstructor
@Slf4j
@Validated
public class MovieController {
    private final MovieService movieService;

    @GetMapping(MOVIE_NOW_SHOWING_PATH)
    public ResponseEntity<ApiResponse<PagedResponse<MovieSummary>>>
    getNowShowingMovies(@Valid @ModelAttribute MovieFilterRequest request) { // Get data form Query string

        log.info("GET /api/movies/now-showing - Request: {}", request);
        PagedResponse<MovieSummary> movies = movieService.getMoviesByStatus(request, MovieStatus.NOW_SHOWING);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Movies now showing"));

    }

    @GetMapping(MOVIE_COMING_SOON_PATH)
    public ResponseEntity<ApiResponse<PagedResponse<MovieSummary>>>
    getComingSoonMovies(@Valid @ModelAttribute MovieFilterRequest request) {

        log.info("GET /api/movies/coming-soon");
        PagedResponse<MovieSummary> movies = movieService.getMoviesByStatus(request, MovieStatus.COMING_SOON);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, movies, "Movies coming soon"));

    }

    @GetMapping(MOVIE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<MovieDetail>> getMovieDetail(@PathVariable @Min(1) Integer movieId) {
        MovieDetail detail = movieService.getMovieDetail(movieId);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, detail, "Movie detail"));
    }
}
