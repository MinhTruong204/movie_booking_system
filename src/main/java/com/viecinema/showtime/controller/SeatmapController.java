package com.viecinema.showtime.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.showtime.dto.SeatmapResponse;
import com.viecinema.showtime.service.SeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Parameter;
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

    /**
     * GET /api/v1/showtimes/{showtimeId}/seatmap
     *
     * Lấy sơ đồ ghế của một suất chiếu
     * - Trả về layout ghế theo hàng/cột
     * - Bao gồm trạng thái từng ghế (available, booked, held)
     * - Nếu user đã login, ghế đang được user hold sẽ có status "held_by_you"
     */
    @GetMapping(SHOWTIMES_SEATMAP_PATH)
    public ResponseEntity<ApiResponse<SeatmapResponse>> getSeatmap(
            @PathVariable Integer showtimeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("GET /api/showtimes/{}/seatmap", showtimeId);

        // Lấy userId từ authentication (null nếu chưa login)
        Integer currentUserId = extractUserId(userDetails);

        SeatmapResponse seatmap = seatmapService.getSeatmap(showtimeId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.SEATMAP_RETRIEVED,seatmap)
        );
    }

    /**
     * Extract userId từ UserDetails
     * Có thể customize tùy theo implementation của UserDetailsService
     */
    private Integer extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        // Giả sử username là email, cần lookup userId
        // Hoặc nếu UserDetails đã chứa userId thì lấy trực tiếp
        // Đây là placeholder - cần adjust theo implementation thực tế
        try {
            // Option 1: Nếu dùng custom UserDetails chứa userId
            // return ((CustomUserDetails) userDetails).getUserId();

            // Option 2: Parse từ username nếu là userId
            return Integer.parseInt(userDetails.getUsername());
        } catch (Exception e) {
            log.debug("Could not extract userId from userDetails: {}", e.getMessage());
            return null;
        }
    }
}
