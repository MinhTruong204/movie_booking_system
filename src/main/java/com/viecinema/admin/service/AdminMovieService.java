package com.viecinema.admin.service;

import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.movie.dto.request.CreateMovieRequest;
import com.viecinema.movie.dto.request.MovieFilterRequest;
import com.viecinema.movie.dto.request.UpdateMovieRequest;
import com.viecinema.movie.dto.response.AdminMovieResponse;
import com.viecinema.movie.dto.response.PagedResponse;
import com.viecinema.movie.entity.Actor;
import com.viecinema.movie.entity.Director;
import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.entity.Movie;
import com.viecinema.movie.mapper.MovieMapper;
import com.viecinema.movie.repository.ActorRepository;
import com.viecinema.movie.repository.DirectorRepository;
import com.viecinema.movie.repository.GenreRepository;
import com.viecinema.movie.repository.MovieRepository;
import com.viecinema.movie.repository.MovieSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final MovieMapper movieMapper;

    // ========== CREATE ==========

    /**
     * Tạo phim mới.
     * Sau khi lưu, evict toàn bộ cache movies để user thấy dữ liệu mới.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "moviesByStatus", allEntries = true),
            @CacheEvict(value = "movieDetails", allEntries = true)
    })
    public AdminMovieResponse createMovie(CreateMovieRequest request) {
        log.info("Admin: Creating new movie with title '{}'", request.getTitle());

        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(request.getDuration())
                .ageRating(request.getAgeRating())
                .language(request.getLanguage())
                .subtitle(request.getSubtitle())
                .producer(request.getProducer())
                .releaseDate(request.getReleaseDate())
                .endDate(request.getEndDate())
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                .bannerUrl(request.getBannerUrl())
                .build();

        // Gán status (default NOW_SHOWING nếu không truyền)
        if (request.getStatus() != null) {
            movie.setStatus(request.getStatus());
        }

        // Resolve relationships
        resolveAndSetGenres(movie, request.getGenreIds());
        resolveAndSetActors(movie, request.getActorIds());
        resolveAndSetDirectors(movie, request.getDirectorIds());

        Movie saved = movieRepository.save(movie);
        log.info("Admin: Movie created with ID {}", saved.getMovieId());
        return movieMapper.toAdminMovieResponse(saved);
    }

    // ========== READ ==========

    /**
     * Lấy danh sách phim cho admin với filter + phân trang.
     * Admin thấy tất cả phim (bao gồm COMING_SOON, NOW_SHOWING, ENDED).
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminMovieResponse> getAllMovies(MovieFilterRequest request) {
        log.info("Admin: Fetching all movies with filters: {}", request);

        Specification<Movie> spec = Specification
                .where(MovieSpecification.isNotDeleted())
                .and(MovieSpecification.fetchGenres())
                .and(MovieSpecification.hasKeyWords(request.getKeyword()))
                .and(MovieSpecification.hasGenres(request.getGenreIds()))
                .and(MovieSpecification.hasStatus(request.getStatus()));

        Pageable pageable = request.toPageable();
        Page<Movie> page = movieRepository.findAll(spec, pageable);

        List<AdminMovieResponse> content = page.getContent()
                .stream()
                .map(movieMapper::toAdminMovieResponse)
                .collect(Collectors.toList());

        return PagedResponse.<AdminMovieResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Lấy chi tiết 1 phim theo ID (chỉ phim chưa bị xóa).
     */
    @Transactional(readOnly = true)
    public AdminMovieResponse getMovieById(Integer id) {
        log.info("Admin: Fetching movie detail for ID {}", id);
        Movie movie = movieRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + id + " not found"));
        return movieMapper.toAdminMovieResponse(movie);
    }

    // ========== UPDATE ==========

    /**
     * Cập nhật phim theo kiểu partial update — chỉ field không null mới được ghi đè.
     * Evict cache sau khi cập nhật thành công.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "moviesByStatus", allEntries = true),
            @CacheEvict(value = "movieDetails", key = "#id")
    })
    public AdminMovieResponse updateMovie(Integer id, UpdateMovieRequest request) {
        log.info("Admin: Updating movie ID {}", id);

        Movie movie = movieRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + id + " not found"));

        // Partial update — chỉ ghi đè nếu field không null
        if (request.getTitle() != null)       movie.setTitle(request.getTitle());
        if (request.getDescription() != null) movie.setDescription(request.getDescription());
        if (request.getDuration() != null)    movie.setDuration(request.getDuration());
        if (request.getAgeRating() != null)   movie.setAgeRating(request.getAgeRating());
        if (request.getLanguage() != null)    movie.setLanguage(request.getLanguage());
        if (request.getSubtitle() != null)    movie.setSubtitle(request.getSubtitle());
        if (request.getProducer() != null)    movie.setProducer(request.getProducer());
        if (request.getReleaseDate() != null) movie.setReleaseDate(request.getReleaseDate());
        if (request.getEndDate() != null)     movie.setEndDate(request.getEndDate());
        if (request.getStatus() != null)      movie.setStatus(request.getStatus());
        if (request.getPosterUrl() != null)   movie.setPosterUrl(request.getPosterUrl());
        if (request.getTrailerUrl() != null)  movie.setTrailerUrl(request.getTrailerUrl());
        if (request.getBannerUrl() != null)   movie.setBannerUrl(request.getBannerUrl());

        // Update relationships nếu được cung cấp (null = không thay đổi)
        if (request.getGenreIds() != null)    resolveAndSetGenres(movie, request.getGenreIds());
        if (request.getActorIds() != null)    resolveAndSetActors(movie, request.getActorIds());
        if (request.getDirectorIds() != null) resolveAndSetDirectors(movie, request.getDirectorIds());

        Movie updated = movieRepository.save(movie);
        log.info("Admin: Movie ID {} updated successfully", id);
        return movieMapper.toAdminMovieResponse(updated);
    }

    // ========== DELETE ==========

    /**
     * Soft delete phim.
     * Hibernate @SQLDelete sẽ tự động set deleted_at = NOW() thay vì DELETE thật.
     * Evict cache liên quan sau khi xóa.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "moviesByStatus", allEntries = true),
            @CacheEvict(value = "movieDetails", key = "#id")
    })
    public void deleteMovie(Integer id) {
        log.info("Admin: Soft-deleting movie ID {}", id);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + id + " not found"));

        movieRepository.delete(movie); // Trigger @SQLDelete → UPDATE movies SET deleted_at = NOW()
        log.info("Admin: Movie ID {} soft-deleted successfully", id);
    }

    // ========== RESTORE ==========

    /**
     * Khôi phục phim đã bị soft-delete.
     * Dùng native query để bỏ qua @Where(deleted_at IS NULL).
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "moviesByStatus", allEntries = true),
            @CacheEvict(value = "movieDetails", key = "#id")
    })
    public AdminMovieResponse restoreMovie(Integer id) {
        log.info("Admin: Restoring movie ID {}", id);

        // Tìm phim kể cả đã xóa (dùng native query)
        Movie movie = movieRepository.findByIdIncludeDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + id + " not found"));

        if (movie.getDeletedAt() == null) {
            throw new IllegalStateException("Movie with ID " + id + " is not deleted");
        }

        movieRepository.restoreById(id);
        log.info("Admin: Movie ID {} restored successfully", id);

        // Reload lại sau khi restore để trả về dữ liệu mới nhất
        Movie restored = movieRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie with ID " + id + " not found after restore"));
        return movieMapper.toAdminMovieResponse(restored);
    }

    // ========== PRIVATE HELPERS ==========

    private void resolveAndSetGenres(Movie movie, List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            movie.setGenres(new HashSet<>());
            return;
        }
        List<Genre> genres = genreRepository.findAllById(genreIds);
        movie.setGenres(new HashSet<>(genres));
    }

    private void resolveAndSetActors(Movie movie, List<Integer> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) {
            movie.setActors(new HashSet<>());
            return;
        }
        List<Actor> actors = actorRepository.findAllByIdIn(actorIds);
        movie.setActors(new HashSet<>(actors));
    }

    private void resolveAndSetDirectors(Movie movie, List<Integer> directorIds) {
        if (directorIds == null || directorIds.isEmpty()) {
            movie.setDirectors(new HashSet<>());
            return;
        }
        List<Director> directors = directorRepository.findAllByIdIn(directorIds);
        movie.setDirectors(new HashSet<>(directors));
    }
}
