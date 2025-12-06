package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeatRow {
    private String rowLabel;      // A, B, C...
    private Integer rowIndex;     // 0, 1, 2...
    private List<SeatInfo> seats;
}
