package com.viecinema.showtime.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.showtime.dto.response.SeatmapResponse;
import com.viecinema.showtime.service.SeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.SHOWTIMES_PATH;
import static com.viecinema.common.constant.ApiConstant.SHOWTIMES_SEATMAP_PATH;

@RestController
@RequestMapping(SHOWTIMES_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Showtimes", description = "Query available showtimes with optional filters")
public class SeatmapController {

    private final SeatmapService seatmapService;

    @Operation(
            summary = "Get seatmap for a showtime",
            description = "Returns the seat layout for a specific showtime, including seat availability status (available, held, booked). Held seats belonging to the current user are marked distinctly.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seatmap retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    @GetMapping(SHOWTIMES_SEATMAP_PATH)
    public ResponseEntity<ApiResponse<SeatmapResponse>> getSeatmap(
            @Parameter(description = "ID of the showtime", required = true, example = "1")
            @PathVariable Integer showtimeId,
            @CurrentUser UserPrincipal currentUser
    ) {
        log.info("GET /api/showtimes/{}/seatmap by user {}", showtimeId, currentUser.getId());

        Integer currentUserId = currentUser.getId();
        SeatmapResponse seatmap = seatmapService.getSeatmap(showtimeId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, seatmap, "Seatmap")
        );
    }
}


