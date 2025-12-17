package com.viecinema.showtime.dto;

import com.viecinema.movie.dto.GenreDto;
import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.entity.MovieGenre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
