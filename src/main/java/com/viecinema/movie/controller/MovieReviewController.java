package com.viecinema.movie.controller;

import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.movie.dto.request.CreateReviewRequest;
import com.viecinema.movie.dto.response.ReviewResponse;
import com.viecinema.movie.service.MovieReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.viecinema.common.constant.ApiConstant.REVIEW_PATH;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_CREATE;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@RestController
@RequestMapping(REVIEW_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Movie Reviews", description = "Submit and view movie reviews")
public class MovieReviewController {

    private final MovieReviewService reviewService;

    @Operation(
            summary = "Submit a movie review",
            description = "Creates a review for a movie. If the authenticated user has a verified PAID booking for the movie, the review will be marked as verified and bonus loyalty points will be awarded automatically.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Review created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid review data or user has already reviewed this movie"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} submitting review for movie {}", currentUser.getId(), request.getMovieId());

        ReviewResponse response = reviewService.createReview(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(RESOURCE_CREATE, response, "Review"));
    }

    @Operation(
            summary = "Get reviews for a movie",
            description = "Returns a paginated list of approved reviews for the specified movie, ordered by most recent first. This endpoint is public and does not require authentication."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByMovie(
            @PathVariable Integer movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getReviewsByMovie(movieId, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(RESOURCE_RETRIEVED, reviews, "Reviews"));
    }
}
