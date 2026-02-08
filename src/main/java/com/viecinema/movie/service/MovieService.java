package com.viecinema.movie.service;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.dto.MovieDetail;
import com.viecinema.movie.dto.MovieSummary;
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
    public MovieDetail getMovieDetail(Integer movieId) {
        log.info("Fetching movie detail for ID: {}", movieId);

        Movie movie = movieRepository.findByIdWithDetails(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with Id " + movieId + " not found"));
        return movieMapper.toMovieDetailDto(movie);
    }

    private Specification<Movie> buildSpecification(MovieFilterRequest request, MovieStatus status) {
        return Specification
                .where(MovieSpecification.hasStatus(status))
                .and(MovieSpecification.isNotDeleted())
                .and(MovieSpecification.fetchGenres())
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
}
