package com.viecinema.admin.controller;

import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.showtime.dto.request.CreateShowtimeRequest;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.dto.request.UpdateShowtimeRequest;
import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import com.viecinema.showtime.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.viecinema.common.constant.ApiConstant.ADMIN_SHOWTIMES_PATH;

/**
 * REST Controller cho Admin Showtime Management.
 * Tất cả endpoints yêu cầu role ADMIN.
 */
@Slf4j
@RestController
@RequestMapping(ADMIN_SHOWTIMES_PATH)
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Showtime Management", description = "CRUD operations for managing movie showtimes (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminShowtimeController {

        private final ShowtimeService showtimeService;

        @Operation(summary = "Create a new showtime", description = "Creates a new movie showtime with room overlap checks and automatic seat status initialization.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Showtime created successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or overlapping showtime"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie or Room not found")
        })
        @PostMapping
        public ResponseEntity<ApiResponse<ShowtimeDetailResponse>> createShowtime(
                        @Valid @RequestBody CreateShowtimeRequest request) {
                log.info("POST {} - Request: {}", ADMIN_SHOWTIMES_PATH, request);

                ShowtimeDetailResponse response = showtimeService.createShowtime(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.success(ApiMessage.RESOURCE_CREATE, response, "Showtime"));
        }

        @Operation(summary = "Get showtime detail by ID", description = "Returns detailed information of a specific showtime by ID.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<ShowtimeDetailResponse>> getShowtimeById(
                        @Parameter(description = "ID of the showtime", required = true, example = "1") @PathVariable Integer id) {
                log.info("GET {}/{}", ADMIN_SHOWTIMES_PATH, id);

                ShowtimeDetailResponse response = showtimeService.getShowtimeById(id);
                return ResponseEntity.ok(ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, response, "Showtime"));
        }

        @Operation(summary = "Get all showtimes (Admin filter)", description = "Returns showtimes with optional admin filters (movie, cinema, room, date, active status).")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtimes retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        @GetMapping
        public ResponseEntity<ApiResponse<List<ShowtimeDetailResponse>>> getShowtimes(
                        @Valid ShowtimeFilterRequest request) {
                log.info("GET {} - Filter: {}", ADMIN_SHOWTIMES_PATH, request);

                List<ShowtimeDetailResponse> response = showtimeService.findAllShowtime(request);
                return ResponseEntity.ok(ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, response, "Showtimes"));
        }

        @Operation(summary = "Update an existing showtime", description = "Updates showtime details with overlap checking and booked seat safety rules.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime updated successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error, overlap conflict, or invalid modification on booked showtime"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime, Movie, or Room not found")
        })
        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<ShowtimeDetailResponse>> updateShowtime(
                        @Parameter(description = "ID of the showtime", required = true, example = "1") @PathVariable Integer id,
                        @Valid @RequestBody UpdateShowtimeRequest request) {
                log.info("PUT {}/{} - Request: {}", ADMIN_SHOWTIMES_PATH, id, request);

                ShowtimeDetailResponse response = showtimeService.updateShowtime(id, request);
                return ResponseEntity.ok(ApiResponse.success(ApiMessage.RESOURCE_UPDATED, response, "Showtime"));
        }

        @Operation(summary = "Delete / Deactivate a showtime", description = "Soft-deletes a showtime. Fails if tickets have already been booked for this showtime.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime deleted successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot delete showtime with booked seats"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteShowtime(
                        @Parameter(description = "ID of the showtime", required = true, example = "1") @PathVariable Integer id) {
                log.info("DELETE {}/{}", ADMIN_SHOWTIMES_PATH, id);

                showtimeService.deleteShowtime(id);
                return ResponseEntity.ok(ApiResponse.successWithoutData(ApiMessage.RESOURCE_DELETED, "Showtime"));
        }
}
