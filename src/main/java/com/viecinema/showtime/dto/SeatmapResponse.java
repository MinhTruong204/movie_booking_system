package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeatmapResponse {
    private ShowtimeInfo showtimeInfo;
    private RoomInfo roomInfo;
    private List<SeatTypeInfo> seatTypes;
    private SeatLayout seatLayout;
    private SeatSummary summary;
}
