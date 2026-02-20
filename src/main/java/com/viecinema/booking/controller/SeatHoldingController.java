package com.viecinema.booking.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.dto.request.ReleaseSeatRequest;
import com.viecinema.booking.dto.response.HoldSeatsResponse;
import com.viecinema.booking.service.SeatHoldingService;
import com.viecinema.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.*;
import static com.viecinema.common.constant.ApiMessage.HOLD_SEAT;
import static com.viecinema.common.constant.ApiMessage.RELEASE_SEAT;

@RestController
@RequestMapping(BOOKING_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Create and manage ticket bookings")
public class SeatHoldingController {

    private final SeatHoldingService seatHoldingService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Hold seats temporarily",
            description = "Temporarily reserves the specified seats for the authenticated user for a limited time (e.g. 10 minutes). Must be done before creating a booking.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seats held successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or seats already taken"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping(HOLD_SEATS_PATH)
    public ResponseEntity<ApiResponse<HoldSeatsResponse>> holdSeats(
            @Valid @RequestBody HoldSeatsRequest request,
            @CurrentUser UserPrincipal userPrincipal) {

        log.info("API POST /api/bookings/hold-seats - request: {}", request);
        log.info("User : {}", userPrincipal.getId());
        HoldSeatsResponse response = seatHoldingService.holdSeats(request, userPrincipal.getId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(HOLD_SEAT, response));
    }

    @Operation(
            summary = "Release all held seats for current user",
            description = "Releases all seats currently held by the authenticated user across all showtimes.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seats released successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping(RELEASE_SEATS_PATH)
    public ResponseEntity<ApiResponse<Void>> releaseUserSeats(
            @CurrentUser UserPrincipal userPrincipal) {

        seatHoldingService.releaseUserSeats(userPrincipal.getId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.successWithoutData(RELEASE_SEAT));
    }

    @Operation(
            summary = "Release a specific held seat",
            description = "Releases a single held seat. Admin users may force-release any seat regardless of who holds it. Regular users can only release their own seats.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat released successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to release this seat")
    })
    @PostMapping(RELEASE_SEAT_PATH)
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @Valid @RequestBody ReleaseSeatRequest request,
            @CurrentUser UserPrincipal userPrincipal) {

        Integer userId = userPrincipal.getId();

        boolean force = Boolean.TRUE.equals(request.getForce()) && securityUtils.userHasAdminRole(userPrincipal);

        seatHoldingService.releaseSeat(request.getShowtimeId(), request.getSeatId(), userId, force);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.successWithoutData(RELEASE_SEAT));
    }
}


