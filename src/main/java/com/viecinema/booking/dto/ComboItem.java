package com.viecinema.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComboItem {
    @NotNull
    private Integer comboId;

    @Min(value = 1, message = "The quantity must be at least 1")
    @Max(value = 20, message = "The quantity must not exceed 20")
    private Integer quantity;
}
