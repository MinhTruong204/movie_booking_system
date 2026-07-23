package com.viecinema.showtime.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShowtimeRequest {

    @NotNull(message = "Movie ID is required")
    private Integer movieId;

    @NotNull(message = "Room ID is required")
    private Integer roomId;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price must be greater than or equal to 0")
    private BigDecimal basePrice;

    private Boolean isActive;
}
