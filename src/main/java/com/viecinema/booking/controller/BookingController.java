package com.viecinema.booking.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.booking.dto.CalculateBookingRequest;
import com.viecinema.booking.dto.CalculateBookingResponse;
import com.viecinema.booking.service.BookingCalculationService;
import com.viecinema.common.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.BOOKING_PATH;
import static com.viecinema.common.constant.ApiConstant.CAlCULATE_PATH;
import static com.viecinema.common.constant.ApiMessage.BOOKING_DETAILS_RETRIEVED;
import static com.viecinema.common.constant.ApiMessage.COMBOS_RETRIEVED;

@RestController
@RequestMapping(BOOKING_PATH)
@RequiredArgsConstructor
public class BookingController {

    private final BookingCalculationService calculationService;
    private final AuthUtil authUtil;

    @PostMapping(CAlCULATE_PATH)
    public ResponseEntity<ApiResponse<CalculateBookingResponse>> calculateBooking(
            @Valid @RequestBody CalculateBookingRequest request,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Integer userId = authUtil.extractUserId((userDetails));
        CalculateBookingResponse response = calculationService.calculateBooking(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(BOOKING_DETAILS_RETRIEVED,response));
    }
}