package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomInfo {
    private Integer roomId;
    private String roomName;
    private Integer cinemaId;
    private String cinemaName;
    private String cinemaAddress;
    private Integer totalSeats;
    private Integer totalRows;
    private Integer maxSeatsPerRow;
}
