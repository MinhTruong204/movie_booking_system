package com.viecinema.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReleaseSeatRequest {
    @NotNull
    private Integer showtimeId;

    @NotNull
    private Integer seatId;

    /**
     * Nếu client gửi force=true và user có quyền admin, service có thể bỏ qua held_by_user check.
     * Thường để false trên client normal.
     */
    private Boolean force = false;
}
