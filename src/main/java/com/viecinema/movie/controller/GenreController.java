package com.viecinema.movie.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.movie.dto.GenreDto;
import com.viecinema.movie.service.GenreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.viecinema.common.constant.ApiConstant.GENRE_DETAIL_PATH;
import static com.viecinema.common.constant.ApiConstant.GENRE_PATH;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@RestController
@RequestMapping(GENRE_PATH)
@RequiredArgsConstructor
@Slf4j
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GenreDto>>> getAllGenres(
            @RequestParam(required = false, defaultValue = "false")
            Boolean includeCount) {
        log.info("API GET /api/genres called with includeCount={}", includeCount);

        List<GenreDto> genres;

        if (Boolean.TRUE.equals(includeCount)) {
            genres = genreService.getAllGenresWithMovieCount();
        } else {
            genres = genreService.getAllGenres();
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, genres, "Genres")
        );
    }

    @GetMapping(GENRE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<GenreDto>> getGenreById(
            @PathVariable Integer id
    ) {
        log.info("API GET /api/genres/{} called", id);

        GenreDto genre = genreService.getGenreById(id);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, genre, "Genre")
        );
    }
}
