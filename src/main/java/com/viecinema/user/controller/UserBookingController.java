package com.viecinema.user.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.user.dto.UserBookingDto;
import com.viecinema.user.service.UserBookingService;
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
public class UserBookingController {

    private final UserBookingService userBookingService;

    /**
     * API lấy tất cả booking của user hiện tại
     */
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
