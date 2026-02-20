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
@Tag(name = "Bookings", description = "Create and manage ticket bookings")
public class BookingController {

    private final BookingCalculationService calculationService;
    private final BookingService bookingService;

    @Operation(
            summary = "Calculate booking cost",
            description = "Calculates the total price for a booking including seat prices, combo charges, and any applicable promotions. Does NOT create a booking.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calculation successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid booking request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping(CAlCULATE_PATH)
    public ResponseEntity<ApiResponse<CalculateBookingResponse>> calculateBooking(
            @Valid @RequestBody CalculateBookingRequest request,
            @CurrentUser UserPrincipal currentUser) {

        Integer userId = currentUser.getId();
        CalculateBookingResponse response = calculationService.calculateBooking(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, response, "Booking calculation"));
    }

    @Operation(
            summary = "Create a booking",
            description = "Confirms and creates a ticket booking for the authenticated customer. Requires CUSTOMER role. Seats must be held before calling this endpoint.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Booking created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid booking data or seats no longer held"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required")
    })
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

