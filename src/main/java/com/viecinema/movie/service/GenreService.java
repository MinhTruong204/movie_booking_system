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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository genreRepository;

    /**
     * Lấy tất cả thể loại (không có số lượng phim)
     * Cache 30 phút vì dữ liệu ít thay đổi
     *
     * @return Danh sách GenreDto
     */
    @Cacheable(value = "genres", key = "'all'")
    public List<GenreDto> getAllGenres() {
        log.debug("Fetching all genres from database");

        List<Genre> genres = genreRepository.findAll();

        return genres.stream()
                .map(this::convertToDto)
                . collect(Collectors.toList());
    }

    /**
     * Lấy tất cả thể loại KÈM số lượng phim
     * Tối ưu performance bằng single query
     *
     * @return Danh sách GenreDto có movieCount
     */
    @Cacheable(value = "genres", key = "'all-with-count'")
    public List<GenreDto> getAllGenresWithMovieCount() {
        log.debug("Fetching all genres with movie count");

        List<GenreRepository.GenreProjection> projections =
                genreRepository.findAllWithMovieCount();

        return projections.stream()
                .map(this::convertProjectionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thể loại theo ID
     *
     * @param genreId ID của thể loại
     * @return GenreDto hoặc throw exception
     */
    public GenreDto getGenreById(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                . orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thể loại với ID: " + genreId
                ));

        return convertToDto(genre);
    }

    // ========== MAPPER METHODS ==========

    /**
     * Convert Entity sang DTO
     */
    private GenreDto convertToDto(Genre genre) {
        return GenreDto.builder()
                .genreId(genre.getGenreId())
                .name(genre.getName())
                .description(genre.getDescription())
                . build();
    }

    /**
     * Convert Projection sang DTO
     */
    private GenreDto convertProjectionToDto(GenreRepository.GenreProjection projection) {
        return GenreDto.builder()
                .genreId(projection. getGenreId())
                .name(projection.getName())
                .description(projection.getDescription())
                .movieCount(projection. getMovieCount())
                .build();
    }
}