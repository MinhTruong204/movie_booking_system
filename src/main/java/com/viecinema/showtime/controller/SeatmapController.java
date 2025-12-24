package com.viecinema.showtime.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.util.AuthUtil;
import com.viecinema.showtime.dto.response.SeatmapResponse;
import com.viecinema.showtime.service.SeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
public class SeatmapController {

    private final SeatmapService seatmapService;
    private final  AuthUtil authUtil;

    @GetMapping(SHOWTIMES_SEATMAP_PATH)
    public ResponseEntity<ApiResponse<SeatmapResponse>> getSeatmap(
            @PathVariable Integer showtimeId,
            @CurrentUser UserPrincipal userPrincipal
    ) {
        log.info("GET /api/showtimes/{}/seatmap", showtimeId);

        // Lấy userId từ authentication (null nếu chưa login)
        Integer currentUserId = authUtil.extractUserId(userPrincipal);
        SeatmapResponse seatmap = seatmapService.getSeatmap(showtimeId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED,seatmap,"Seatmap")
        );
    }
}
