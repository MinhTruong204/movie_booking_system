package com.viecinema.showtime.service;

import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.dto.ShowtimeGroupByCinemaDto;
import com.viecinema.showtime.dto.ShowtimeGroupByTimeSlotDto;

import java.util.List;

public interface ShowtimeService {
    Object findShowtimes(ShowtimeFilterRequest filter);
    List<ShowtimeDetailResponse> findAllShowtime(ShowtimeFilterRequest filter);
    List<ShowtimeGroupByCinemaDto> findShowtimesGroupByCinema(ShowtimeFilterRequest filter);
    List<ShowtimeGroupByTimeSlotDto> findShowtimesGroupByTimeSlot(ShowtimeFilterRequest filter);
}
