package com.viecinema.movie.repository;

import com.viecinema.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    /**
     * Tìm thể loại theo tên (case-insensitive)
     */
    Optional<Genre> findByNameIgnoreCase(String name);

    /**
     * Lấy danh sách thể loại kèm số lượng phim (tối ưu performance)
     * Sử dụng LEFT JOIN để đếm cả thể loại chưa có phim
     */
    @Query("""
        SELECT g. genreId as genreId, 
               g.name as name, 
               g.description as description,
               COUNT(mg. movie. movieId) as movieCount
        FROM Genre g
        LEFT JOIN MovieGenre mg ON mg.genre. genreId = g.genreId
        LEFT JOIN Movie m ON mg.movie. movieId = m.movieId AND m.deletedAt IS NULL
        GROUP BY g.genreId, g.name, g.description
        ORDER BY g.name ASC
        """)
    List<GenreProjection> findAllWithMovieCount();

    /**
     * Projection interface để nhận kết quả từ query trên
     */
    interface GenreProjection {
        Integer getGenreId();
        String getName();
        String getDescription();
        Long getMovieCount();
    }
}
