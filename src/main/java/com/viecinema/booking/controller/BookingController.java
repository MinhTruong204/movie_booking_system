package com.viecinema.booking.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.booking.dto.request.BookingRequest;
import com.viecinema.booking.dto.request.CalculateBookingRequest;
import com.viecinema.booking.dto.response.BookingResponse;
import com.viecinema.booking.dto.response.CalculateBookingResponse;
import com.viecinema.booking.service.BookingCalculationService;
import com.viecinema.booking.service.BookingService;
import com.viecinema.common.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.*;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_CREATE;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@RestController
@RequestMapping(BOOKING_PATH)
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingCalculationService calculationService;
    private final BookingService bookingService;
    private final AuthUtil authUtil;

    @PostMapping(CAlCULATE_PATH)
    public ResponseEntity<ApiResponse<CalculateBookingResponse>> calculateBooking(
            @Valid @RequestBody CalculateBookingRequest request,
            @CurrentUser UserPrincipal currentUser) {

        Integer userId = currentUser.getId();
        CalculateBookingResponse response = calculationService.calculateBooking(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, response, "Booking calculation"));
    }

    @PostMapping(CREATE_BOOKING_PATH)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody BookingRequest request) {

        log.info("User {} creating booking for showtime {}",
                currentUser.getId(), request.getShowtimeId());

        try {
            BookingResponse response = bookingService.createBooking(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(RESOURCE_CREATE, response, "Booking"));

        } catch (Exception e) {
            log.error("Error creating booking", e);
            throw e;
        }
    }
}