package com.viecinema.showtime.repository;

import com.viecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime,Integer> {
    @Query(value = """
        SELECT 
            s.showtime_id,
            s.movie_id,
            s.room_id,
            s.start_time,
            s.end_time,
            s.base_price,
            s.is_active,
            
            -- Movie info
            m.title AS movie_title,
            m. poster_url AS movie_poster,
            m.duration AS movie_duration,
            m. age_rating AS movie_age_rating,
            
            -- Cinema info
            c.cinema_id,
            c.name AS cinema_name,
            c.address AS cinema_address,
            c.city AS cinema_city,
            
            -- Room info
            r.name AS room_name,
            r.total_seats AS room_total_seats,
            
            -- Seat availability
            COUNT(DISTINCT ss.seat_id) AS total_seats,
            SUM(CASE WHEN ss.status = 'available' THEN 1 ELSE 0 END) AS available_seats,
            SUM(CASE WHEN ss.status = 'booked' THEN 1 ELSE 0 END) AS booked_seats,
            SUM(CASE WHEN ss.status = 'held' THEN 1 ELSE 0 END) AS held_seats
            
        FROM showtimes s
        INNER JOIN movies m ON s.movie_id = m.movie_id
        INNER JOIN rooms r ON s.room_id = r.room_id
        INNER JOIN cinemas c ON r.cinema_id = c.cinema_id
        LEFT JOIN seat_status ss ON s.showtime_id = ss.showtime_id
        
        WHERE 1=1
            AND (:movieId IS NULL OR s.movie_id = :movieId)
            AND (:cinemaId IS NULL OR c.cinema_id = :cinemaId)
            AND (:roomId IS NULL OR s.room_id = :roomId)
            AND (:city IS NULL OR c.city = :city)
            AND (:ignoreTimeFilter = TRUE OR (s.start_time >= :startDateTime AND s.start_time <= :endDateTime))
            AND (:activeOnly = FALSE OR s.is_active = TRUE)
            AND (:futureOnly = FALSE OR s.start_time > NOW())
            AND s.deleted_at IS NULL
            AND m.deleted_at IS NULL
            AND c.deleted_at IS NULL
            AND r.deleted_at IS NULL
            
        GROUP BY s.showtime_id
        ORDER BY 
            CASE WHEN :sortBy = 'START_TIME' THEN s.start_time END ASC,
            CASE WHEN :sortBy = 'PRICE' THEN s.base_price END ASC,
            CASE WHEN :sortBy = 'CINEMA_NAME' THEN c.name END ASC,
            CASE WHEN :sortBy = 'AVAILABLE_SEATS' THEN available_seats END DESC
        """, nativeQuery = true)
    List<Object[]> findShowtimesWithDetails(
            @Param("movieId") Integer movieId,
            @Param("cinemaId") Integer cinemaId,
            @Param("roomId") Integer roomId,
            @Param("city") String city,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("activeOnly") Boolean activeOnly,
            @Param("futureOnly") Boolean futureOnly,
            @Param("sortBy") String sortBy,
            @Param("ignoreTimeFilter") boolean ignoreTimeFilter
    );

    /**
     * Lấy thông tin giá theo loại ghế cho một showtime
     */
    @Query(value = """
        SELECT 
            st.name AS seat_type_name,
            (sh.base_price * st.price_multiplier) AS final_price,
            COUNT(CASE WHEN ss.status = 'available' THEN 1 END) AS available_count
        FROM showtimes sh
        INNER JOIN seat_status ss ON sh.showtime_id = ss.showtime_id
        INNER JOIN seats se ON ss.seat_id = se.seat_id
        INNER JOIN seat_types st ON se.seat_type_id = st.seat_type_id
        WHERE sh.showtime_id = :showtimeId
        GROUP BY st.seat_type_id, st.name, st.price_multiplier, sh.base_price
        ORDER BY final_price ASC
        """, nativeQuery = true)
    List<Object[]> findPricingInfoByShowtime(@Param("showtimeId") Integer showtimeId);

    /**
     * Kiểm tra showtime có tồn tại và active không
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Showtime s " +
            "WHERE s.id = :showtimeId " +
            "AND s.isActive = true " +
            "AND s.deletedAt IS NULL")
    boolean existsByIdAndActive(@Param("showtimeId") Integer showtimeId);

    /**
     * Tìm các showtimes đang diễn ra (để hiển thị "Đang chiếu")
     */
    @Query("SELECT s FROM Showtime s " +
            "WHERE s.startTime <= :now " +
            "AND s.endTime >= :now " +
            "AND s.isActive = true " +
            "AND s.deletedAt IS NULL")
    List<Showtime> findCurrentShowtimes(@Param("now") LocalDateTime now);
}
