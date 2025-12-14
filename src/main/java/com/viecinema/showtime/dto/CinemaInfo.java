package com.viecinema.showtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaInfo {
    private Integer cinemaId;
    private String name;
    private String address;
    private String city;
}
