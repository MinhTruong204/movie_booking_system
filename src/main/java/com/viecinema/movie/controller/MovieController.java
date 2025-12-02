package com.viecinema.movie.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.movie.dto.MovieFilterRequest;
import com.viecinema.movie.dto.MovieSummaryDto;
import com.viecinema.movie.dto.PagedResponse;
import com.viecinema.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.viecinema.common.constant.ApiConstant.MOVIE_NOW_SHOWING_PATH;
import static com.viecinema.common.constant.ApiConstant.MOVIE_PATH;

@RestController
@RequestMapping(MOVIE_PATH)
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "http://localhost:3000",allowCredentials = "true")
public class MovieController {
    private final MovieService movieService;

    @GetMapping(MOVIE_NOW_SHOWING_PATH)
    public ResponseEntity<ApiResponse<PagedResponse<MovieSummaryDto>>>
        getNowShowingMovies(@Valid @ModelAttribute MovieFilterRequest request) { // Get data form Query string

        log. info("GET /api/v1/movies/now-showing - Request: {}", request);
        PagedResponse<MovieSummaryDto> movies = movieService.getNowShowingMovies(request);

        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.USER_RETRIEVED, movies));

    }
}
