package com.viecinema.movie.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenreDto {
    private Integer genreId;
    private String name;
    private String description;
    @JsonProperty("movieCount")
    private Long movieCount;
}
