package com.viecinema.user.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.user.dto.UserBookingDto;
import com.viecinema.user.service.UserBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.viecinema.common.constant.ApiConstant.BOOKINGS_USER_PATH;
import static com.viecinema.common.constant.ApiConstant.BOOKING_PATH;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@Slf4j
@RestController
@RequestMapping(BOOKING_PATH)
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Create and manage ticket bookings")
public class UserBookingController {

    private final UserBookingService userBookingService;

    @Operation(
            summary = "Get my bookings",
            description = "Returns the full booking history of the currently authenticated customer, including ticket details, showtime info, and payment status.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bookings retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required")
    })
    @GetMapping(BOOKINGS_USER_PATH)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<UserBookingDto>>> getMyBookings(
            @CurrentUser UserPrincipal currenttUser) {

        log.info("GET /api/bookings/my-bookings - User:  {}", currenttUser.getUsername());
        List<UserBookingDto> bookings = userBookingService.getAllUserBookings(currenttUser.getId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, bookings, "Bookings"));
    }
}


