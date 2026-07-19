package com.viecinema.movie.service;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.dto.GenreInfo;
import com.viecinema.movie.dto.MovieDetail;
import com.viecinema.movie.dto.MovieSummary;
import com.viecinema.movie.dto.TopMovieDto;
import com.viecinema.movie.dto.request.MovieFilterRequest;
import com.viecinema.movie.dto.response.PagedResponse;
import com.viecinema.movie.entity.Movie;
import com.viecinema.movie.entity.MovieStatistic;
import com.viecinema.movie.mapper.MovieMapper;
import com.viecinema.movie.repository.MovieRepository;
import com.viecinema.movie.repository.MovieSpecification;
import com.viecinema.movie.repository.MovieStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MovieStatisticsRepository movieStatisticsRepository;

    @Cacheable( // Save data to cache, when the same request be sent, use this cache instead query to database
            value = "moviesByStatus",
            key = "#request.toCacheKey(#status)",  // Cache key for distinguish different request
            unless = "#result == null || #result.content.isEmpty()"// don't save to cache if result is null or empty
    )
    public PagedResponse<MovieSummary> getMoviesByStatus(MovieFilterRequest request, MovieStatus status) {
        log.info("Fetching movies with status '{}' and filters: {}", status, request);

        Specification<Movie> spec = buildSpecification(request, status);
        Pageable pageable = request.toPageable();
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        List<Integer> movieIds = moviePage.getContent().stream()
                .map(Movie::getMovieId)
                .collect(Collectors.toList());
        var statsMap = movieStatisticsRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(s -> s.getMovies().getMovieId(), s -> s));

        log.info("Found {} movies out of {} total",
                moviePage.getNumberOfElements(),
                moviePage.getTotalElements()
        );

        List<MovieSummary> content = moviePage.getContent()
                .stream()
                .map(movie -> mapToSummaryWithStats(movie,statsMap.get(movie.getMovieId())))
                .collect(Collectors.toList());

        return PagedResponse.<MovieSummary>builder()
                .content(content)
                .pageNumber(moviePage.getNumber())
                .pageSize(moviePage.getSize())
                .totalElements(moviePage.getTotalElements())
                .totalPages(moviePage.getTotalPages())
                .last(moviePage.isLast())
                .first(moviePage.isFirst())
                .empty(moviePage.isEmpty())
                .build();
    }

    @Cacheable(value = "movieDetails", key = "#movieId", unless = "#result == null")
    public MovieDetail  getMovieDetail(Integer movieId) {
        log.info("Fetching movie detail for ID: {}", movieId);

        Movie movie = movieRepository.findByIdWithDetails(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with Id " + movieId + " not found"));
        return movieMapper.toMovieDetailDto(movie);
    }

    /**
     * Phim được đánh giá cao nhất.
     * Lọc những phim có ít nhất {@code minReviews} reviews, sort theo rating giảm dần.
     *
     * @param limit      số phim tối đa trả về
     * @param minReviews số review tối thiểu để đảm bảo rating đáng tin cậy
     */
    public List<TopMovieDto> getTopRatedMovies(int limit, int minReviews) {
        log.info("Fetching top-rated movies: limit={}, minReviews={}", limit, minReviews);
        return movieStatisticsRepository
                .findTopRated(minReviews, PageRequest.of(0, limit))
                .stream()
                .map(this::toTopMovieDto)
                .collect(Collectors.toList());
    }

    /**
     * Phim nhiều người xem nhất, sort theo total_bookings giảm dần.
     *
     * @param limit số phim tối đa trả về
     */
    public List<TopMovieDto> getMostViewedMovies(int limit) {
        log.info("Fetching most-viewed movies: limit={}", limit);
        return movieStatisticsRepository
                .findMostViewed(PageRequest.of(0, limit))
                .stream()
                .map(this::toTopMovieDto)
                .collect(Collectors.toList());
    }

    /**
     * Phim xuất sắc: đạt ngưỡng rating VÀ lượt đặt vé cùng lúc.
     *
     * @param limit       số phim tối đa trả về
     * @param minRating   ngưỡng average_rating tối thiểu (ví dụ: 4.0)
     * @param minBookings ngưỡng total_bookings tối thiểu (ví dụ: 100)
     */
    public List<TopMovieDto> getOutstandingMovies(int limit, double minRating, int minBookings) {
        log.info("Fetching outstanding movies: limit={}, minRating={}, minBookings={}", limit, minRating, minBookings);
        return movieStatisticsRepository
                .findOutstanding(minRating, minBookings, PageRequest.of(0, limit))
                .stream()
                .map(this::toTopMovieDto)
                .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPERS ==========

    private Specification<Movie> buildSpecification(MovieFilterRequest request, MovieStatus status) {
        return Specification
                .where(MovieSpecification.hasStatus(status))
                .and(MovieSpecification.isNotDeleted())
                .and(MovieSpecification.fetchGenres())
                .and(MovieSpecification.hasKeyWords(request.getKeyword()))
                .and(MovieSpecification.hasGenres(request.getGenreIds()));
    }

    private MovieSummary mapToSummaryWithStats(Movie movie, MovieStatistic statistic) {
        MovieSummary dto = movieMapper.toMovieSummaryDto(movie);
        if (statistic != null) {
            dto.setAverageRating(statistic.getAverageRating() != null ? statistic.getAverageRating().doubleValue() : null);
            dto.setTotalReviews(statistic.getTotalReviews());
        }
        return dto;
    }

    /** Chuyển MovieStatistic → TopMovieDto (Movie đã được JOIN FETCH sẵn). */
    private TopMovieDto toTopMovieDto(MovieStatistic stat) {
        Movie movie = stat.getMovies();
        List<GenreInfo> genres = movie.getGenres().stream()
                .map(g -> new GenreInfo(g.getGenreId(), g.getName(), null, null))
                .collect(Collectors.toList());

        return TopMovieDto.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .ageRating(movie.getAgeRating())
                .language(movie.getLanguage())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .bannerUrl(movie.getBannerUrl())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus() != null ? movie.getStatus().name() : null)
                .genres(genres)
                .averageRating(stat.getAverageRating() != null ? stat.getAverageRating().doubleValue() : null)
                .totalReviews(stat.getTotalReviews())
                .totalBookings(stat.getTotalBookings())
                .build();
    }
}


