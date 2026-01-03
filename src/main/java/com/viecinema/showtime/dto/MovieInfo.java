package com.viecinema.showtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieInfo {
    private Integer movieId;
    private String title;
    private String posterUrl;
    private Integer duration;
    private String ageRating;
    private List<String> genres = new ArrayList<>();
}
