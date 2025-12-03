package com.viecinema.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorDto {
    private Integer directorId;
    private String name;
    private String nationality;
    private String photoUrl;
}
