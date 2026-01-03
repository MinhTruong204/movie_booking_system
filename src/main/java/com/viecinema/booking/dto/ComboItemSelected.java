package com.viecinema.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComboItemSelected {
    @NotNull
    private Integer comboId;

    @Min(value = 1, message = "The quantity must be at least 1")
    @Max(value = 20, message = "The quantity must not exceed 20")
    private Integer quantity;
}
