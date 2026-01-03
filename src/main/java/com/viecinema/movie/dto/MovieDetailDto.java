package com.viecinema.movie.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetailDto {

    // Basic Info
    private Integer movieId;
    private String title;
    private String description;
    private Integer duration;
    private String ageRating;
    private String language;
    private String subtitle;
    private String producer;

    // Dates
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String status;

    private String posterUrl;
    private String trailerUrl;

    private List<GenreDto> genres;
    private List<DirectorDto> directors;
    private List<ActorDto> actors;

    // Statistics
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer totalBookings;
    private BigDecimal totalRevenue;
}
