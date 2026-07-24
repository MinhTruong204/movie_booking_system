package com.viecinema.movie.repository;

import com.viecinema.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer>, JpaSpecificationExecutor<Movie> {

    @Query("""
            SELECT DISTINCT m FROM Movie m
            LEFT JOIN FETCH m.genres
            LEFT JOIN FETCH m.actors
            LEFT JOIN FETCH m.directors
            WHERE m.movieId = :movieId
            AND m.deletedAt IS NULL
            """)
    Optional<Movie> findByIdWithDetails(@Param("movieId") Integer movieId);

    /**
     * Tìm phim kể cả đã soft-delete — dùng cho admin (restore, xem chi tiết phim đã xóa).
     * Bỏ qua @Where(clause = "deleted_at IS NULL") bằng cách dùng native query.
     */
    @Query(value = """
            SELECT * FROM movies
            WHERE movie_id = :movieId
            """, nativeQuery = true)
    Optional<Movie> findByIdIncludeDeleted(@Param("movieId") Integer movieId);

    /**
     * Restore phim đã soft-delete: set deleted_at = NULL.
     */
    @Modifying
    @Query(value = "UPDATE movies SET deleted_at = NULL WHERE movie_id = :movieId", nativeQuery = true)
    void restoreById(@Param("movieId") Integer movieId);
}

