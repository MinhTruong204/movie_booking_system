package com.viecinema.movie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Null field will not be included in JSON response
public class MovieSummary {
    private Integer movieId;
    private String title;
    private String description;
    private Integer duration;
    private String ageRating;
    private String language;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    private String status;
    private String posterUrl;
    private String trailerUrl;
    private List<GenreInfo> genres;

    //    For now showing movie
    private Double averageRating;
    private Integer totalReviews;
    //    For coming soon movie
    private Integer daysUntilRelease;
}
