package com.viecinema.showtime.dto;

import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeGroupByCinemaDto {

    private Integer cinemaId;
    private String cinemaName;
    private String address;
    private String city;
    private Integer totalShowtimes;
    private Integer totalAvailableSeats;

    private List<ShowtimeDetailResponse> showtimes;

    public void calculateStats() {
        if (showtimes != null) {
            this.totalShowtimes = showtimes.size();
            this.totalAvailableSeats = showtimes.stream()
                    .mapToInt(st -> st.getSeatAvailability() != null
                            ? st.getSeatAvailability().getAvailableSeats() : 0).sum();
        }
    }
}
