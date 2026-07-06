package com.viecinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response sau khi user tạo review thành công.
 */
@Data
@Builder
public class ReviewResponse {

    private Integer reviewId;
    private Integer movieId;
    private Integer userId;
    private String userFullName;

    private Integer rating;
    private Integer ratingVideo;
    private Integer ratingAudio;
    private Integer ratingSubtitle;

    private String comment;

    /** Đánh review đã được xác thực qua booking PAID hay chưa */
    private Boolean isVerifiedBooking;

    /** Điểm BONUS đã được cộng nếu review hợp lệ (null = chưa cộng) */
    private Integer bonusPointsAwarded;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
