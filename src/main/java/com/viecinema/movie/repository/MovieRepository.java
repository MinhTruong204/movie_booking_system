package com.viecinema.movie.repository;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie,Integer>, JpaSpecificationExecutor<Movie> {

    @Query("""
        SELECT DISTINCT m FROM Movie m
        LEFT JOIN FETCH m.genres
        LEFT JOIN FETCH m.actors
        LEFT JOIN FETCH m. directors
        WHERE m.movieId = :movieId
        AND m.deletedAt IS NULL
        """)
    Optional<Movie> findByIdWithDetails(@Param("movieId") Integer movieId);

    @Query("""
        SELECT COUNT(DISTINCT m) FROM Movie m
        JOIN m.genres g
        WHERE m.status = 'NOW_SHOWING'
        AND m.deletedAt IS NULL
        AND g.genreId = :genreId
        """)
    Long countNowShowingByGenre(@Param("genreId") Integer genreId);

    boolean existsByMovieIdAndStatusAndDeletedAtIsNull(
            Integer movieId,
            MovieStatus status
    );

}
