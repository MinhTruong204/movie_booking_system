package com.viecinema.showtime.service;

import com.viecinema.showtime.dto.SeatmapResponse;

public interface SeatmapService {
    SeatmapResponse getSeatmap(Integer showtimeId, Integer currentUserId);
}
