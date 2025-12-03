package com.viecinema.movie.service;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.dto.MovieDetailDto;
import com.viecinema.movie.dto.MovieFilterRequest;
import com.viecinema.movie.dto.MovieSummaryDto;
import com.viecinema.movie.dto.PagedResponse;
import com.viecinema.movie.entity.Movie;
import com.viecinema.movie.mapper.MovieMapper;
import com.viecinema.movie.repository.MovieStatisticsRepository;
import com.viecinema.movie.repository.MovieRepository;
import com.viecinema.movie.repository.MovieSpecification;
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
public class MovieServiceImpl implements MovieService{
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MovieStatisticsRepository movieStatisticsRepository;

    @Override
    @Cacheable( // Save data to cache, when the same request be sent, use this cache instead querry to database
            value = "moviesByStatus",
            key = "#request.toCacheKey(#status)",  // Cache key for distinguish different request
            unless = "#result == null || #result.content.isEmpty()"// dont save to cache if result is null or empty
    )
    public PagedResponse<MovieSummaryDto> getMoviesByStatus(MovieFilterRequest request, MovieStatus status) {
        log.info("Fetching movies with status '{}' and filters: {}", status, request);

        request.validate();

        Specification<Movie> spec = buildSpecification(request, status);
        Pageable pageable = request.toPageable();
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        log.info("✅ Found {} movies out of {} total",
                moviePage.getNumberOfElements(),
                moviePage.getTotalElements()
        );

        // Convert to DTO
        List<MovieSummaryDto> content = moviePage.getContent()
                .stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());

        return PagedResponse.<MovieSummaryDto>builder()
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "movieDetails", key = "#movieId", unless = "#result == null")
    public MovieDetailDto getMovieDetail(Integer movieId) {
        log.info("Fetching movie detail for ID: {}", movieId);

        Movie movie = movieRepository.findByIdWithDetails(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with Id " +movieId + " not found"));
        return movieMapper.toMovieDetailDto(movie);
    }

    private Specification<Movie> buildSpecification(MovieFilterRequest request, MovieStatus status) {
        return Specification
                // Filter cơ bản
                .where(MovieSpecification.hasStatus(status))
                .and(MovieSpecification.isNotDeleted())

                // Eager fetch để tránh N+1
                .and(MovieSpecification.fetchGenres())
                // Filter từ request
                .and(MovieSpecification.hasGenres(request.getGenreIds()));
    }

    private MovieSummaryDto convertToSummaryDto(Movie movie) {
        MovieSummaryDto dto = movieMapper.toMovieSummaryDto(movie);

        // Set average rating and total reviews
        movieStatisticsRepository.findById(movie.getMovieId())
                .ifPresent(stats -> {
                    dto.setAverageRating(stats.getAverageRating() != null
                            ? stats.getAverageRating(). doubleValue()
                            : null);
                    dto.setTotalReviews(stats.getTotalReviews());
                });

        return dto;
    }

//    private List<GenreDto> convertGenres(java.util.Set<Genre> genres) {
//        if (genres == null || genres.isEmpty()) {
//            return List.of();
//        }
//
//        return genres.stream()
//                . map(genre -> GenreDto.builder()
//                        . genreId(genre.getGenreId())
//                        .name(genre.getName())
//                        .description(genre.getDescription())
//                        .build())
//                .collect(Collectors.toList());
//    }
}
