package com.viecinema.booking.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.dto.response.HoldSeatsResponse;
import com.viecinema.booking.dto.request.ReleaseSeatRequest;
import com.viecinema.booking.service.SeatHoldingService;
import com.viecinema.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
public class SeatHoldingController {

    private final SeatHoldingService seatHoldingService;

    @PostMapping(HOLD_SEATS_PATH)
    public ResponseEntity<ApiResponse<HoldSeatsResponse>> holdSeats(
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("API POST /api/bookings/hold-seats - request: {}", request);
        Integer userId = extractUserId(userDetails);
        log.info("User : {}",userId);
        HoldSeatsResponse response = seatHoldingService.holdSeats(request, userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(HOLD_SEAT, response));
    }

    @PostMapping(RELEASE_SEATS_PATH)
    public ResponseEntity<ApiResponse<Void>> releaseSeats(
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer userId = extractUserId(userDetails);
        seatHoldingService.releaseUserSeats(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.successWithoutData(RELEASE_SEAT));
    }

    @PostMapping(RELEASE_SEAT_PATH)
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @Valid @RequestBody ReleaseSeatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer userId = extractUserId(userDetails);

        boolean force = Boolean.TRUE.equals(request.getForce()) && userHasAdminRole(userDetails);

        seatHoldingService.releaseSeat(request.getShowtimeId(), request.getSeatId(), userId, force);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.successWithoutData(RELEASE_SEAT));
    }

    private boolean userHasAdminRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Integer extractUserId(UserDetails userDetails) {
    if (userDetails instanceof CustomUserDetails customUserDetails) {
        return customUserDetails.getUserId();
    }
    // This should not happen in a normal flow if authentication is set up correctly
    log.error("UserDetails is not an instance of CustomUserDetails. Actual type: {}", userDetails.getClass().getName());
    throw new ValidationException("Unable to extract user ID from principal.");
    }

}
