package com.viecinema.movie.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.movie.dto.GenreInfo;
import com.viecinema.movie.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Genres", description = "Retrieve movie genres and their details")
public class GenreController {

    private final GenreService genreService;

    @Operation(
            summary = "Get all genres",
            description = "Returns a list of all available movie genres. Pass `includeCount=true` to also include the number of movies per genre."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Genres retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<List<GenreInfo>>> getAllGenres(
            @Parameter(description = "When true, includes movie count per genre", example = "false")
            @RequestParam(required = false, defaultValue = "false")
            Boolean includeCount) {
        log.info("API GET /api/genres called with includeCount={}", includeCount);

        List<GenreInfo> genres;

        if (Boolean.TRUE.equals(includeCount)) {
            genres = genreService.getAllGenreWithMovieCount();
        } else {
            genres = genreService.getAllGenre();
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, genres, "Genres")
        );
    }

    @Operation(
            summary = "Get genre by ID",
            description = "Returns details for a specific genre identified by its ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Genre retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Genre not found")
    })
    @SecurityRequirements
    @GetMapping(GENRE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<GenreInfo>> getGenreById(
            @Parameter(description = "ID of the genre", required = true, example = "1")
            @PathVariable Integer id
    ) {
        log.info("API GET /api/genres/{} called", id);

        GenreInfo genre = genreService.getGenreById(id);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, genre, "Genres")
        );
    }
}


