package com.viecinema.movie.service;

import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.dto.GenreDto;
import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository genreRepository;

    @Cacheable(value = "genres", key = "'all'")
    public List<GenreDto> getAllGenres() {
        log.debug("Fetching all genres from database");

        List<Genre> genres = genreRepository.findAll();

        return genres
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Cacheable(value = "genres", key = "'all-with-count'")
    public List<GenreDto> getAllGenresWithMovieCount() {
        log.debug("Fetching all genres with movie count");

        List<GenreRepository.GenreProjection> projections =
                genreRepository.findAllWithMovieCount();

        return projections
                .stream()
                .map(this::convertProjectionToDto)
                .toList();
    }

    public GenreDto getGenreById(Integer genreId) {
        log.debug("Fetching genres by ID: {}", genreId);
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre with ID: " + genreId));
        return convertToDto(genre);
    }

    // ========== MAPPER METHODS ==========

    private GenreDto convertToDto(Genre genre) {
        return GenreDto.builder()
                .genreId(genre.getGenreId())
                .name(genre.getName())
                .description(genre.getDescription())
                .build();
    }

    private GenreDto convertProjectionToDto(GenreRepository.GenreProjection projection) {
        return GenreDto.builder()
                .genreId(projection.getGenreId())
                .name(projection.getName())
                .description(projection.getDescription())
                .movieCount(projection.getMovieCount())
                .build();
    }
}