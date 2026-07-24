package com.viecinema.movie.dto.request;

import com.viecinema.common.enums.MovieStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO cho request cập nhật phim.
 * Tất cả fields đều optional — chỉ các field không null mới được cập nhật (partial update).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMovieRequest {

    @Size(max = 255, message = "Title không được vượt quá 255 ký tự")
    private String title;

    private String description;

    @Min(value = 1, message = "Duration phải lớn hơn 0 phút")
    @Max(value = 600, message = "Duration không được vượt quá 600 phút")
    private Integer duration;

    @Pattern(regexp = "^(P|C13|C16|C18)$", message = "Age rating phải là một trong: P, C13, C16, C18")
    private String ageRating;

    @Size(max = 100, message = "Language không được vượt quá 100 ký tự")
    private String language;

    @Size(max = 100, message = "Subtitle không được vượt quá 100 ký tự")
    private String subtitle;

    @Size(max = 255, message = "Producer không được vượt quá 255 ký tự")
    private String producer;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private MovieStatus status;

    @Size(max = 255, message = "Poster URL không được vượt quá 255 ký tự")
    private String posterUrl;

    @Size(max = 255, message = "Trailer URL không được vượt quá 255 ký tự")
    private String trailerUrl;

    @Size(max = 255, message = "Banner URL không được vượt quá 255 ký tự")
    private String bannerUrl;

    /** Nếu không null → replace toàn bộ danh sách genres của phim. */
    private List<Integer> genreIds;

    /** Nếu không null → replace toàn bộ danh sách actors của phim. */
    private List<Integer> actorIds;

    /** Nếu không null → replace toàn bộ danh sách directors của phim. */
    private List<Integer> directorIds;
}
