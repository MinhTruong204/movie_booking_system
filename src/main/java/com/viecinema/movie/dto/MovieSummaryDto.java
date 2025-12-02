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
@JsonInclude(JsonInclude. Include.NON_NULL) // Null field will not be included in JSON response
public class MovieSummaryDto {
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
    private List<GenreDto> genres;

//    For now showing movie
    private Double averageRating;
    private Integer totalReviews;
    // Helper method
    public String getDurationFormatted() {
        if (duration == null) return null;
        int hours = duration / 60;
        int minutes = duration % 60;
        return String.format("%dh %02dm", hours, minutes);
    }

//    For coming soon movie
    private Integer daysUntilRelease;

}
