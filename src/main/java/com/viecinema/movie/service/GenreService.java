package com.viecinema.movie.service;

import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.dto.GenreBasicProjection;
import com.viecinema.movie.dto.GenreFullProjection;
import com.viecinema.movie.dto.GenreInfo;
import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.mapper.GenreMapper;
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
    private final GenreMapper genreMapper;

    @Cacheable(value = "genres", key = "'all'")
    public List<GenreInfo> getAllGenre() {
        log.debug("Fetching all genres from database");

        List<GenreBasicProjection> genres = genreRepository.findAllGenre();

        return genres
                .stream()
                .map(genreMapper::toGenreDto)
                .toList();
    }

    @Cacheable(value = "genres", key = "'all-with-count'")
    public List<GenreInfo> getAllGenreWithMovieCount() {
        log.debug("Fetching all genres with movie count");

        List<GenreFullProjection> projections =
                genreRepository.findAllGenreWithMovieCount();

        return projections
                .stream()
                .map(genreMapper::toGenreDto)
                .toList();
    }

    public GenreInfo getGenreById(Integer genreId) {
        log.debug("Fetching genres by ID: {}", genreId);
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre with ID: " + genreId));
        return genreMapper.toGenreDto(genre);
    }
}