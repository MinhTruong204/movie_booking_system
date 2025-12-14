package com.viecinema.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComboItem {
    @NotNull
    private Integer comboId;

    @Min(1)
    @Max(20)
    private Integer quantity;
}
