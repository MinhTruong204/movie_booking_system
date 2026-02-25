package com.viecinema.showtime.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ShowtimeInfo {
    private Integer showtimeId;
    private Integer movieId;
    private String movieTitle;
    private String posterUrl;
    private String bannerUrl;
    private Integer duration;
    private String ageRating;
    private String cinemaName;
    private String roomName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    private BigDecimal basePrice;
}
