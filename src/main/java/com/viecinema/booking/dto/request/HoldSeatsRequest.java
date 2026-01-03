package com.viecinema.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HoldSeatsRequest {

    @NotNull(message = "Showtime ID không được để trống")
    @Positive(message = "Showtime ID phải là số dương")
    private Integer showtimeId;

    @NotNull(message = "Danh sách ghế không được để trống")
    @Size(min = 1, max = 10, message = "Số ghế phải từ 1 đến 10")
    private List<@Positive(message = "Seat ID phải là số dương") Integer> seatIds;

    @Max(value = 900, message = "Thời gian giữ tối đa 15 phút (900 giây)")
    private Integer holdDurationSeconds = 600; // Default 10 phút
}