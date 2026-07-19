package com.viecinema.movie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO dung chung cho 3 API xep hang phim:
 * top-rated, most-viewed, outstanding.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopMovieDto {

    private Integer movieId;
    private String title;
    private String description;
    private Integer duration;
    private String ageRating;
    private String language;
    private String posterUrl;
    private String trailerUrl;
    private String bannerUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    private String status;
    private List<GenreInfo> genres;

    /** Diem danh gia trung binh */
    private Double averageRating;

    /** Tong so luot review */
    private Integer totalReviews;

    /** Tong so luot dat ve (luot xem) */
    private Integer totalBookings;
}