package com.viecinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.viecinema.movie.dto.ActorInfo;
import com.viecinema.movie.dto.DirectorDto;
import com.viecinema.movie.dto.GenreInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO dành riêng cho Admin — bao gồm đầy đủ audit fields (createdAt, updatedAt, deletedAt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminMovieResponse {

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
    private String bannerUrl;

    // Relationships
    private List<GenreInfo> genres;
    private List<DirectorDto> directors;
    private List<ActorInfo> actors;

    // Audit fields (chỉ admin mới thấy)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;
}
