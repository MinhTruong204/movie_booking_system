package com.viecinema.showtime.service;

import com.viecinema.showtime.dto.ShowtimeDetailResponse;
import com.viecinema.showtime.dto.ShowtimeFilterRequest;
import com.viecinema.showtime.dto.ShowtimeGroupByCinemaDto;
import com.viecinema.showtime.dto.ShowtimeGroupByTimeSlotDto;

import java.util.List;

public interface ShowtimeService {
    /**
     * Tìm showtimes theo filter và group theo yêu cầu
     *
     * @param filter Bộ lọc
     * @return Object có thể là List hoặc Grouped DTO tùy theo groupBy
     */
    Object findShowtimes(ShowtimeFilterRequest filter);

    /**
     * Tìm showtimes không group (flat list)
     */
    List<ShowtimeDetailResponse> findShowtimesList(ShowtimeFilterRequest filter);

    /**
     * Tìm showtimes group theo rạp
     */
    List<ShowtimeGroupByCinemaDto> findShowtimesGroupByCinema(ShowtimeFilterRequest filter);

    /**
     * Tìm showtimes group theo khung giờ
     */
    List<ShowtimeGroupByTimeSlotDto> findShowtimesGroupByTimeSlot(ShowtimeFilterRequest filter);

    /**
     * Lấy chi tiết một showtime
     */
    ShowtimeDetailResponse getShowtimeDetail(Integer showtimeId);
}
