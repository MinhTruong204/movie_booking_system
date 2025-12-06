package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeatLayout {
    private List<SeatRow> rows;
    private String screenPosition; // top, bottom
}
