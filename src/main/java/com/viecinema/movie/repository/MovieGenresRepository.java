package com.viecinema.movie.repository;

import com.viecinema.movie.entity.MovieGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieGenresRepository extends JpaRepository<MovieGenre, Integer> {

    /**
     * Lấy tất cả genres của một movie
     */
    @Query("SELECT mg FROM MovieGenre mg " +
            "LEFT JOIN FETCH mg.genre " +
            "WHERE mg. movie.movieId = :movieId")
    List<MovieGenre> findByMovieId(@Param("movieId") Integer movieId);

    /**
     * Lấy genres của nhiều movies (batch loading)
     */
    @Query("SELECT mg FROM MovieGenre mg " +
            "LEFT JOIN FETCH mg.genre " +
            "LEFT JOIN FETCH mg.movie " +
            "WHERE mg.movie. movieId IN :movieIds")
    List<MovieGenre> findByMovieIds(@Param("movieIds") List<Integer> movieIds);
}
