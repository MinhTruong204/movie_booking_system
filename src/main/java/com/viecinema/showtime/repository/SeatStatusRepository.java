package com.viecinema.showtime.repository;

import com.viecinema.showtime.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, Integer> {

    /**
     * Lấy tất cả trạng thái ghế của một suất chiếu
     */
    @Query("""
        SELECT ss FROM SeatStatus ss
        JOIN FETCH ss.seat s
        JOIN FETCH s.seatType st
        LEFT JOIN FETCH ss.heldByUser u
        WHERE ss.showtime.id = :showtimeId
    """)
    List<SeatStatus> findByShowtimeId(@Param("showtimeId") Integer showtimeId);

    /**
     * Lấy trạng thái một ghế cụ thể
     */
    @Query("""
        SELECT ss FROM SeatStatus ss
        WHERE ss.showtime.id = :showtimeId
        AND ss.seat.seatId = :seatId
    """)
    Optional<SeatStatus> findByShowtimeIdAndSeatId(
            @Param("showtimeId") Integer showtimeId,
            @Param("seatId") Integer seatId
    );

    /**
     * Đếm ghế theo trạng thái
     */
    @Query("""
        SELECT ss.status, COUNT(ss)
        FROM SeatStatus ss
        WHERE ss.showtime.id = :showtimeId
        GROUP BY ss.status
    """)
    List<Object[]> countByShowtimeIdGroupByStatus(@Param("showtimeId") Integer showtimeId);

    /**
     * Giải phóng các ghế held đã hết hạn
     */
    @Modifying
    @Query("""
        UPDATE SeatStatus ss
        SET ss.status = 'AVAILABLE',
            ss.heldByUser = null,
            ss.heldUntil = null,
            ss. updatedAt = CURRENT_TIMESTAMP
        WHERE ss.status = 'HELD'
        AND ss. heldUntil < :now
    """)
    int releaseExpiredHeldSeats(@Param("now") LocalDateTime now);

    /**
     * Kiểm tra xem đã có seat_status cho showtime chưa
     */
    @Query("SELECT COUNT(ss) > 0 FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId")
    boolean existsByShowtimeId(@Param("showtimeId") Integer showtimeId);
}
