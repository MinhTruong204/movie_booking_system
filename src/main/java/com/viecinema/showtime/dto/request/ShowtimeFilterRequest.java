package com.viecinema.showtime.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeFilterRequest {

    @Min(value = 1, message = "Movie ID must be positive")
    private Integer movieId;

    @Min(value = 1, message = "Cinema ID must be positive")
    private Integer cinemaId;

    private Integer roomId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String city;
    @Builder.Default
    private Boolean activeOnly = true;
    @Builder.Default
    private Boolean futureOnly = true;
    @Builder.Default
    private GroupBy groupBy = GroupBy.CINEMA;
    @Builder.Default
    private SortBy sortBy = SortBy.START_TIME;
    @Builder.Default
    private Boolean includeAvailableSeats = true;

    // Cờ nội bộ để bỏ qua hoàn toàn bộ lọc thời gian
    @JsonIgnore // Không hiển thị trong JSON response/request
    @Builder.Default
    private boolean ignoreTimeFilter = false;

    public enum GroupBy {
        CINEMA,      // Group theo rạp
        TIMESLOT,    // Group theo khung giờ (sáng, chiều, tối)
        ROOM,        // Group theo phòng chiếu
        NONE         // Không group, trả về flat list
    }

    public enum SortBy {
        START_TIME,     // Sắp xếp theo giờ chiếu
        PRICE,          // Sắp xếp theo giá
        CINEMA_NAME,    // Sắp xếp theo tên rạp
        AVAILABLE_SEATS // Sắp xếp theo số ghế trống
    }

    public boolean isValid() {
        return movieId != null || cinemaId != null;
    }

    public LocalDateTime getStartDateTime() {
        if (ignoreTimeFilter) {
            // Trả về một ngày rất xa trong quá khứ để lấy tất cả
            return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return searchDate.atStartOfDay();
    }

    public LocalDateTime getEndDateTime() {
        if (ignoreTimeFilter) {
            // Trả về một ngày rất xa trong tương lai để lấy tất cả
            return LocalDateTime.of(9999, 12, 31, 23, 59);
        }
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return searchDate.atTime(23, 59, 59);
    }
}
