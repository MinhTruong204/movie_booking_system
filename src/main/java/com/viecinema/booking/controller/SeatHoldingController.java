package com.viecinema.booking.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.booking.dto.request.HoldSeatsRequest;
import com.viecinema.booking.dto.request.ReleaseSeatRequest;
import com.viecinema.booking.dto.response.HoldSeatsResponse;
import com.viecinema.booking.service.SeatHoldingService;
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
public class SeatHoldingController {

    private final SeatHoldingService seatHoldingService;

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

    @PostMapping(RELEASE_SEATS_PATH)
    public ResponseEntity<ApiResponse<Void>> releaseSeats(
            @CurrentUser UserPrincipal userPrincipal) {

        seatHoldingService.releaseUserSeats(userPrincipal.getId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.successWithoutData(RELEASE_SEAT));
    }

    @PostMapping(RELEASE_SEAT_PATH)
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @Valid @RequestBody ReleaseSeatRequest request,
            @CurrentUser UserPrincipal userPrincipal) {

        Integer userId = userPrincipal.getId();

        boolean force = Boolean.TRUE.equals(request.getForce()) && userHasAdminRole(userPrincipal);

        seatHoldingService.releaseSeat(request.getShowtimeId(), request.getSeatId(), userId, force);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.successWithoutData(RELEASE_SEAT));
    }

    private boolean userHasAdminRole(UserPrincipal userPrincipal) {
        return userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
