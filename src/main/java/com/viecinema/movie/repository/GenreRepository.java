package com.viecinema.movie.repository;

import com.viecinema.movie.dto.GenreBasicProjection;
import com.viecinema.movie.dto.GenreFullProjection;
import com.viecinema.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    @Query("""
            SELECT g.genreId as genreId,
                   g.name as name,
                   g.description as description,
                   COUNT(mg.movie.movieId) as movieCount
            FROM Genre g
            LEFT JOIN MovieGenre mg ON mg.genre.genreId = g.genreId
            LEFT JOIN Movie m ON mg.movie.movieId = m.movieId AND m.deletedAt IS NULL
            GROUP BY g.genreId, g.name, g.description
            ORDER BY g.name ASC
            """)
    List<GenreFullProjection> findAllGenreWithMovieCount();

    @Query("SELECT g.genreId as genreId, g.name as name, g.description as description FROM Genre g")
    List<GenreBasicProjection> findAllGenre();
}
