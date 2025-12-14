package com.viecinema.showtime.service;

import com.viecinema.showtime.dto.response.SeatmapResponse;

public interface SeatmapService {
    SeatmapResponse getSeatmap(Integer showtimeId, Integer currentUserId);
}
