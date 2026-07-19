package com.viecinema.movie.repository;

import com.viecinema.movie.entity.MovieStatistic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieStatisticsRepository extends JpaRepository<MovieStatistic, Integer> {

    /**
     * Phim được đánh giá cao:
     * lọc theo số lượng review tối thiểu, sort theo average_rating giảm dần.
     */
    @Query("""
            SELECT s FROM MovieStatistic s
            JOIN FETCH s.movies m
            WHERE s.totalReviews >= :minReviews
              AND m.deletedAt IS NULL
            ORDER BY s.averageRating DESC
            """)
    List<MovieStatistic> findTopRated(@Param("minReviews") int minReviews, Pageable pageable);

    /**
     * Phim nhiều người xem nhất: sort theo total_bookings giảm dần.
     */
    @Query("""
            SELECT s FROM MovieStatistic s
            JOIN FETCH s.movies m
            WHERE m.deletedAt IS NULL
            ORDER BY s.totalBookings DESC
            """)
    List<MovieStatistic> findMostViewed(Pageable pageable);

    /**
     * Phim xuất sắc: đồng thời đạt ngưỡng rating VÀ lượt đặt vé.
     */
    @Query("""
            SELECT s FROM MovieStatistic s
            JOIN FETCH s.movies m
            WHERE s.averageRating >= :minRating
              AND s.totalBookings >= :minBookings
              AND m.deletedAt IS NULL
            ORDER BY s.averageRating DESC, s.totalBookings DESC
            """)
    List<MovieStatistic> findOutstanding(
            @Param("minRating") double minRating,
            @Param("minBookings") int minBookings,
            Pageable pageable
    );
}

