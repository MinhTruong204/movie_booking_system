package com.viecinema.movie.dto.request;

import com.viecinema.common.enums.MovieStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static com.viecinema.common.constant.MessageConstant.MOVIE_FILTER_PAGE_ERROR;
import static com.viecinema.common.constant.MessageConstant.MOVIE_FILTER_SIZE_ERROR;
import static com.viecinema.common.constant.ValidationConstant.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieFilterRequest {
    private List<Integer> genreIds;

    @Min(value = 0, message = MOVIE_FILTER_PAGE_ERROR)
    @Builder.Default
    private Integer page = 0;

    @Min(value = MOVIE_PAGE_MIN_SIZE, message = MOVIE_FILTER_SIZE_ERROR)
    @Max(value = MOVIE_PAGE_MAX_SIZE, message = MOVIE_FILTER_SIZE_ERROR)
    @Builder.Default
    private Integer size = 12;

    @Pattern(
            regexp = FILTER_MOVIE_REGEX,
            message = "Sort format invalid "
    )
    @Builder.Default
    private String sort = "releaseDate,desc";

    private String keyword;
    private String language;
    private String ageRating;
    private String city;

    // ============== HELPER METHODS ==============

    //    Convert from request to pageable
    public Pageable toPageable() {
        String[] sortParams = sort.split(",");
        String property = sortParams[0];
        Sort.Direction direction = sortParams.length > 1
                && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    public void validate() {
        if (genreIds != null && !genreIds.isEmpty()) {
            genreIds.removeIf(id -> id == null || id <= 0);
        }
    }

    public boolean hasFilters() {
        return (genreIds != null && !genreIds.isEmpty())
                || keyword != null
                || language != null
                || ageRating != null
                || city != null;
    }

    public String toCacheKey(MovieStatus status) {
        return String.format("movies:%s:%s:%d:%d:%s:%s:%s:%s:%s",
                status.name(),
                genreIds != null ? genreIds.toString() : "all",
                page,
                size,
                sort,
                keyword != null ? keyword : "all",
                language != null ? language : "all",
                ageRating != null ? ageRating : "all",
                city != null ? city : "all"
        );
    }

    @Override
    public String toString() {
        return "MovieFilterRequest{" +
                "genreIds=" + genreIds +
                ", page=" + page +
                ", size=" + size +
                ", sort='" + sort + '\'' +
                ", keyword='" + keyword + '\'' +
                ", language='" + language + '\'' +
                ", ageRating='" + ageRating + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
